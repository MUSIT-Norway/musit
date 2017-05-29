package controllers.analysis

import models.analysis.SampleStatuses.{Intact, SampleStatus}
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{ActorId, MuseumId, Museums, ObjectUUID}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.test.Helpers._

class SampleObjectControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid          = MuseumId(99)
  val token        = BearerToken(FakeUsers.testAdminToken)
  val adminId      = ActorId.unsafeFromString(FakeUsers.testAdminId)
  val dummyActorId = ActorId.generate()

  val responsibleActor = ActorId.generate().asString

  val parentObject = ObjectUUID.generate()

  // scalastyle:off
  def createSaveJSON(
      maybeId: Option[ObjectUUID] = None,
      maybeParent: Option[ObjectUUID] = None,
      parentObjectType: ObjectType = CollectionObject,
      isExtracted: Boolean = true,
      status: SampleStatus = Intact,
      doneBy: ActorId,
      doneDate: String,
      maybeSampleId: Option[String],
      maybeExtId: Option[String],
      maybeNote: Option[String]
  ) = {
    val js1 = Json.obj(
      "parentObject" -> Json
        .obj("objectId" -> maybeParent, "objectType" -> parentObjectType),
      "isExtracted"          -> isExtracted,
      "museumId"             -> Museums.Test.id.underlying,
      "status"               -> status.key,
      "responsible"          -> dummyActorId,
      "doneByStamp"          -> Json.obj("user" -> doneBy, "date" -> doneDate),
      "sampleTypeId"         -> 37,
      "size"                 -> Json.obj("unit" -> "cm3", "value" -> 12.0),
      "container"            -> "box",
      "storageMedium"        -> "alcohol",
      "leftoverSample"       -> 1,
      "originatedObjectUuid" -> parentObject.asString
    )
    val js2 = maybeId.map(i => js1 ++ Json.obj("objectId"       -> i.asString)).getOrElse(js1)
    val js4 = maybeSampleId.map(s => js2 ++ Json.obj("sampleId" -> s)).getOrElse(js2)
    val js5 = maybeExtId
      .map(i => js4 ++ Json.obj("externalId" -> Json.obj("value" -> i)))
      .getOrElse(js4)
    maybeNote.map(n => js5 ++ Json.obj("note" -> n)).getOrElse(js5)
  }
  // scalastyle:on

  def validateSampleObject(
      parentObjectType: ObjectType = CollectionObject,
      isExtracted: Boolean = true,
      expectedParent: Option[String] = None,
      expectedSampleId: Option[String] = None,
      expectedExtId: Option[String] = None,
      expectedNote: Option[String] = None,
      js: JsValue
  ): Unit = {
    (js \ "parentObject" \ "objectType").as[ObjectType] mustBe parentObjectType
    (js \ "originatedObjectUuid").as[ObjectUUID] mustBe parentObject
    (js \ "isExtracted").as[Boolean] mustBe isExtracted
    (js \ "parentObject" \ "objectId").asOpt[String] mustBe expectedParent
    (js \ "sampleNum").asOpt[Int].getOrElse(0) must be > 0
    (js \ "sampleId").asOpt[String] mustBe expectedSampleId
    (js \ "externalId" \ "value").asOpt[String] mustBe expectedExtId
    (js \ "note").asOpt[String] mustBe expectedNote
  }

  def validateSampleObject(expected: JsValue, actual: JsValue): Unit = {
    val parentObjectType = (expected \ "parentObject" \ "objectType").as[ObjectType]
    val isExtracted      = (expected \ "isExtracted").as[Boolean]
    val expParent        = (expected \ "parentObject" \ "objectId").asOpt[String]
    val expSampleId      = (expected \ "sampleId").asOpt[String]
    val expExtId         = (expected \ "externalId" \ "value").asOpt[String]
    val expNote          = (expected \ "note").asOpt[String]

    validateSampleObject(
      parentObjectType,
      isExtracted,
      expParent,
      expSampleId,
      expExtId,
      expNote,
      actual
    )
  }

  val baseUrl      = (mid: Int) => s"/$mid/samples"
  val addUrl       = baseUrl
  val forMuseumUrl = baseUrl
  val updateUrl    = (mid: Int) => (oid: String) => s"${baseUrl(mid)}/$oid"
  val getUrl       = updateUrl
  val childrenUrl  = (mid: Int) => (oid: String) => s"${getUrl(mid)(oid)}/children"
  val deleteUrl    = (mid: Int) => (oid: String) => s"${baseUrl(mid)}/$oid"

  def getAllForTestMuseum = {
    val res = wsUrl(forMuseumUrl(mid)).withHeaders(token.asHeader).get().futureValue
    res.status mustBe OK
    res
  }

  def updateJson[A](
      js: JsValue,
      updatePath: JsPath,
      value: A
  )(implicit f: Format[A]): JsObject = {
    val trans = updatePath.json.update(__.read[A].map(_ => Json.toJson(value)))

    js.transform(trans) match {
      case JsSuccess(jso, _) => jso
      case err: JsError      => throw JsResultException(err.errors)
    }
  }

  "Invoking the sample object controller API" should {

    "successfully add a few new SampleObject" in {
      val cd = DateTime.now.minusWeeks(2).toString("yyyy-MM-dd")
      val jsarr = (1 to 10).map { index =>
        createSaveJSON(
          maybeParent = Some(parentObject),
          doneBy = dummyActorId,
          doneDate = cd,
          maybeSampleId = Some(s"sample$index"),
          maybeExtId = Some(s"ext$index"),
          maybeNote = Some("This is a sample note")
        )
      }

      forAll(jsarr) { js =>
        wsUrl(addUrl(mid))
          .withHeaders(token.asHeader)
          .post(js)
          .futureValue
          .status mustBe CREATED
      }
    }

    "list all objects for a museum" in {
      val res     = getAllForTestMuseum
      val objects = res.json.as[JsArray].value
      objects.size mustBe 10

      forAll(objects.zipWithIndex) {
        case (obj, index) =>
          validateSampleObject(
            expectedParent = Some(parentObject.asString),
            expectedSampleId = Some(s"sample${index + 1}"),
            expectedExtId = Some(s"ext${index + 1}"),
            expectedNote = Some("This is a sample note"),
            js = obj
          )
      }
    }

    "return the sample object with the given ID" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(getUrl(mid)(objectId)).withHeaders(token.asHeader).get().futureValue

      res.status mustBe OK

      res.json mustBe expJs
    }

    "update a specific sample object" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val ujs = updateJson[String](expJs, __ \ "note", "Updated note")

      val res =
        wsUrl(updateUrl(mid)(objectId)).withHeaders(token.asHeader).put(ujs).futureValue

      res.status mustBe OK
      validateSampleObject(ujs, res.json)
    }

    "delete a specific sample object" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(deleteUrl(mid)(objectId)).withHeaders(token.asHeader).get().futureValue

      res.status mustBe OK
    }

    "return 404 when trying to delete a non-existing sample objectid" in {
      val objectId = "123e4567-e89b-12d3-a456-426655440000"

      val res =
        wsUrl(deleteUrl(mid)(objectId)).withHeaders(token.asHeader).get().futureValue

      res.status mustBe NOT_FOUND
    }

    " return 400 when trying to delete a sample object with an invalid UUID" in {
      val objectId = "123"

      val res =
        wsUrl(deleteUrl(mid)(objectId)).withHeaders(token.asHeader).get().futureValue

      res.status mustBe BAD_REQUEST
    }
  }

}
