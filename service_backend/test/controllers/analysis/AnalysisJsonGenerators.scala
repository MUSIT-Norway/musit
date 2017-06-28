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

  val dummyObjectId = ObjectUUID.generate()

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
    (actual \ "objectId").asOpt[String] mustBe expectedObject.map(_.asString)
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
