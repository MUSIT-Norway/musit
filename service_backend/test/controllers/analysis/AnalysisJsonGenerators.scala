package controllers.analysis

import models.analysis.events.AnalysisExtras.ExtraAttributes
import models.analysis.events.AnalysisResults._
import models.analysis.events.{Analysis, AnalysisCollection, Category}
import models.analysis.Size
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models.{ActorId, ObjectUUID, OrgId}
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpec}
import no.uio.musit.time
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import play.api.libs.json._

trait AnalysisJsonGenerators {

  // test data
  val liquidChromatography = 1
  val tomographyTypeId     = 7

  val validObjectUUIDs = Seq(
    ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"),
    ObjectUUID.unsafeFromString("376d41e7-c463-45e8-9bde-7a2c9844637e"),
    ObjectUUID.unsafeFromString("2350578d-0bb0-4601-92d4-817478ad0952"),
    ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"),
    ObjectUUID.unsafeFromString("bf53f481-1db3-4474-98ee-c94df31ec251"),
    ObjectUUID.unsafeFromString("373bb138-ed93-472b-ad57-ccb77ab8c151"),
    ObjectUUID.unsafeFromString("62272640-e29e-4af4-a537-3c49b5f1cf42"),
    ObjectUUID.unsafeFromString("f4a189c3-4d8f-4258-9000-b23282814278"),
    ObjectUUID.unsafeFromString("67965e71-27ee-4ef0-ad66-e7e321882f33"),
    ObjectUUID.unsafeFromString("6f9db6a5-f994-4498-8ebc-c9ba75c51ce8"),
    ObjectUUID.unsafeFromString("b17b8735-2350-4de9-b812-93753b1eeb8d"),
    ObjectUUID.unsafeFromString("215542d3-48c9-44af-a6ea-4c494da54fe0"),
    ObjectUUID.unsafeFromString("065b9812-0f22-4ba4-bac2-7d7cbc850dcc"),
    ObjectUUID.unsafeFromString("6ca2bf73-fa17-4d41-a8da-ab9f46a7494b"),
    ObjectUUID.unsafeFromString("21dadc0d-50ca-41ea-9b48-90fdec515148"),
    ObjectUUID.unsafeFromString("7b2e3bd6-b699-4671-bd50-1d964342f531"),
    ObjectUUID.unsafeFromString("40898454-069e-41c9-9551-946a1e693f59"),
    ObjectUUID.unsafeFromString("f56bd93f-49b6-4111-b6c8-bd84a14ea98e"),
    ObjectUUID.unsafeFromString("b71d3bd7-28e3-4790-b4f6-98664e2385ba"),
    ObjectUUID.unsafeFromString("6ee89241-a404-4086-b373-c42dd0a2e56a"),
    ObjectUUID.unsafeFromString("5f7f0b2b-f2eb-480d-816e-00b140705f2b"),
    ObjectUUID.unsafeFromString("bbf9a3de-9203-4e90-9b04-4475e4f7f749"),
    ObjectUUID.unsafeFromString("29339044-8696-4c76-9b3e-f153ae63d262"),
    ObjectUUID.unsafeFromString("7ae2521e-904c-432b-998c-bb09810310a9"),
    ObjectUUID.unsafeFromString("42b6a92e-de59-4fde-9c46-5c8794be0b34")
  )

  val testObjectUUID = validObjectUUIDs.head

  val adminId = ActorId.unsafeFromString(FakeUsers.testAdminId)

  def createSizeJSON(size: Size): JsObject = Json.obj(
    "value" -> size.value,
    "unit"  -> size.unit
  )

  def appendExtraAttributes[ValueType](
      js: JsObject,
      ea: ExtraAttributes
  ): JsObject = {
    // Taking a shortcut here for generating the different types of ExtraAttributes
    // by using the defined implicit writes for that type. The attributes can be
    // quite complex, and defining generic algorithm for dealing with all the types
    // just isn't worth it.
    js ++ Json.obj("extraAttributes" -> Json.toJson(ea))
  }

  def createSaveAnalysisCollectionJSON(
      typeId: Int = liquidChromatography,
      eventDate: Option[DateTime],
      objects: Seq[ObjectUUID],
      note: Option[String] = None,
      caseNumbers: Option[Seq[String]] = None,
      orgId: Option[OrgId] = None
  ): JsObject = {
    val js1 = Json.obj(
      "analysisTypeId" -> typeId,
      "responsible"    -> adminId,
      "administrator"  -> adminId,
      "completedBy"    -> adminId,
      "status"         -> 1
    )
    val js2 = note.map(n => js1 ++ Json.obj("note" -> n)).getOrElse(js1)
    val js3 = eventDate.map { d =>
      js2 ++ Json.obj("eventDate" -> Json.toJson[DateTime](d))
    }.getOrElse(js2)
    val js4 = caseNumbers.map { cn =>
      js3 ++ Json.obj("caseNumbers" -> JsArray(cn.map(JsString)))
    }.getOrElse(js3)
    val js5 = orgId.map(o => js4 ++ Json.obj("orgId" -> o)).getOrElse(js4)
    js5 ++ Json.obj(
      "objects" -> objects.map { id =>
        Json.obj(
          "objectId"   -> id.asString,
          "objectType" -> "collection"
        )
      },
      "restriction" -> Json.obj(
        "requester"      -> adminId,
        "reason"         -> "secret",
        "expirationDate" -> time.dateTimeNow.plusDays(20)
      )
    )
  }

  def createGenericResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      tpe: String = GenericResult.resultType
  ): JsObject = {

    val js1 = Json.obj("type" -> tpe)
    val js2 = comment.map(c => js1 ++ Json.obj("comment" -> c)).getOrElse(js1)
    extRef.map(er => js2 ++ Json.obj("extRef" -> er)).getOrElse(js2)
  }

  def createDatingResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      age: Option[String]
  ): JsValue = {
    val js1 = createGenericResultJSON(extRef, comment, AgeResult.resultType)
    age.map(a => js1 ++ Json.obj("age" -> a)).getOrElse(js1)
  }

  def createC14ResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      ageEstimate: Option[String],
      standardDeviation: Option[String]
  ): JsValue = {
    val js1 = createGenericResultJSON(extRef, comment, RadioCarbonResult.resultType)
    val js2 = ageEstimate.map(a => js1 ++ Json.obj("ageEstimate" -> a)).getOrElse(js1)
    standardDeviation.map(s => js2 ++ Json.obj("standardDeviation" -> s)).getOrElse(js2)
  }

  def createMeasurementResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      tpe: Option[String],
      size: Option[Size],
      precision: Option[String],
      method: Option[String]
  ): JsValue = {
    val js1 = createGenericResultJSON(extRef, comment, MeasurementResult.resultType)
    val js2 = tpe.map(a => js1 ++ Json.obj("measurementType" -> a)).getOrElse(js1)
    val js3 = size.map(s => js2 ++ Json.obj("size" -> createSizeJSON(s))).getOrElse(js2)
    val js4 = tpe.map(a => js3 ++ Json.obj("precision" -> a)).getOrElse(js3)
    method.map(a => js4 ++ Json.obj("method" -> a)).getOrElse(js4)
  }

  def createExtractionResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      medium: Option[String],
      concentration: Option[Size],
      volume: Option[Size]
  ): JsValue = {
    val js1 = createGenericResultJSON(extRef, comment, ExtractionResult.resultType)
    val js2 = medium.map(s => js1 ++ Json.obj("storageMedium" -> s)).getOrElse(js1)
    val js3 =
      concentration.map(c => js2 ++ Json.obj("concentration" -> c)).getOrElse(js2)
    volume.map(v => js3 ++ Json.obj("volume" -> v)).getOrElse(js3)
  }

}

trait AnalysisJsonValidators {
  self: MusitSpec with DateTimeMatchers with AnalysisJsonGenerators =>

  def validateAnalysisType(
      expectedCategory: Category,
      expectedName: String,
      expectedShortName: Option[String],
      expectedExtraAttributes: Option[Map[String, String]],
      actual: JsValue
  ) = {
    (actual \ "id").asOpt[String] must not be empty
    (actual \ "category").as[Int] mustBe expectedCategory.id
    (actual \ "name").as[String] mustBe expectedName
    (actual \ "shortName").asOpt[String] mustBe expectedShortName
    (actual \ "extraAttributes").asOpt[Map[String, String]] mustBe expectedName
  }

  def validateAnalysis(
      expectedId: Long,
      expectedTypeId: Int,
      expectedEventDate: Option[DateTime],
      expectedObject: Option[ObjectUUID],
      expectedParent: Option[Long],
      expectedNote: Option[String],
      actual: JsValue
  ) = {
    (actual \ "type").as[String] mustBe Analysis.discriminator
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "analysisTypeId").as[Int] mustBe expectedTypeId
    (actual \ "eventDate").asOpt[DateTime] mustApproximate expectedEventDate
    (actual \ "affectedThing").asOpt[String] mustBe expectedObject.map(_.asString)
    (actual \ "partOf").asOpt[Long] mustBe expectedParent
    (actual \ "note").asOpt[String] mustBe expectedNote
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
  }

  def validateAnalysisCollection(
      expectedId: Long,
      expectedTypeId: Int,
      expectedEventDate: Option[DateTime],
      expectedNote: Option[String],
      numChildren: Int,
      actual: JsValue
  ) = {
    (actual \ "type").as[String] mustBe AnalysisCollection.discriminator
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "analysisTypeId").as[Int] mustBe expectedTypeId
    (actual \ "eventDate").asOpt[DateTime] mustApproximate expectedEventDate
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
    (actual \ "restriction" \ "requester").as[String] mustBe adminId.asString
    forAll(0 until numChildren) { index =>
      (actual \ "events" \ index \ "type").as[String] mustBe Analysis.discriminator
      (actual \ "events" \ index \ "partOf").as[Long] mustBe expectedId
      (actual \ "events" \ index \ "note").asOpt[String] mustBe expectedNote
    }
  }
}
