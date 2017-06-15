package controllers.analysis

import controllers.storage.MoveObjectUrl
import models.analysis.SampleStatuses.{Intact, SampleStatus}
import no.uio.musit.formatters.DateTimeFormatters._
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, ObjectType}
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SampleObjectControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid          = Museums.Test.id.underlying
  val token        = BearerToken(FakeUsers.testAdminToken)
  val adminId      = FakeUsers.testAdminId
  val dummyActorId = ActorId.generate().asString

  val responsibleActor = ActorId.generate().asString
  val parentObject     = ObjectUUID.generate().asString

  // scalastyle:off
  def createSaveJSON(
      maybeId: Option[String] = None,
      maybeParent: Option[String] = None,
      originatingObject: Option[String] = None,
      parentObjectType: ObjectType = CollectionObjectType,
      isExtracted: Boolean = true,
      status: SampleStatus = Intact,
      doneBy: String,
      doneDate: DateTime,
      maybeSampleId: Option[String],
      maybeExtId: Option[String],
      maybeNote: Option[String]
  ) = {
    val origId: String =
      originatingObject.getOrElse(maybeParent.getOrElse(ObjectUUID.generate().asString))

    val js1 = Json.obj(
      "isExtracted"          -> isExtracted,
      "museumId"             -> mid,
      "status"               -> status.key,
      "responsible"          -> dummyActorId,
      "doneByStamp"          -> Json.obj("user" -> doneBy, "date" -> doneDate),
      "sampleTypeId"         -> 37,
      "size"                 -> Json.obj("unit" -> "cm3", "value" -> 12.0),
      "container"            -> "box",
      "storageMedium"        -> "alcohol",
      "leftoverSample"       -> 1,
      "originatedObjectUuid" -> origId
    )
    val js2 = maybeParent.map { p =>
      js1 ++ Json.obj(
        "parentObject" -> Json.obj(
          "objectId"   -> p,
          "objectType" -> parentObjectType
        )
      )
    }.getOrElse(js1)
    val js3 = maybeId.map(i => js2 ++ Json.obj("objectId"       -> i)).getOrElse(js2)
    val js4 = maybeSampleId.map(s => js3 ++ Json.obj("sampleId" -> s)).getOrElse(js3)
    val js5 = maybeExtId
      .map(i => js4 ++ Json.obj("externalId" -> Json.obj("value" -> i)))
      .getOrElse(js4)
    maybeNote.map(n => js5 ++ Json.obj("note" -> n)).getOrElse(js5)
  }

  def createAndSave(
      cd: DateTime,
      maybeParent: Option[String],
      maybeOrigObject: Option[String],
      numToCreate: Int = 10
  ): Seq[WSResponse] = {
    (1 to numToCreate).map { index =>
      val js = createSaveJSON(
        maybeParent = maybeParent,
        originatingObject = maybeOrigObject,
        doneBy = dummyActorId,
        doneDate = cd,
        maybeSampleId = Some(s"sample$index"),
        maybeExtId = Some(s"ext$index"),
        maybeNote = Some("This is a sample note")
      )
      wsUrl(addUrl(mid)).withHeaders(token.asHeader).post(js).futureValue
    }
  }

  // scalastyle:on

  def validateSampleObject(
      parentObjectType: String = "collection",
      isExtracted: Boolean = true,
      expectedParent: Option[String] = None,
      expectedSampleId: Option[String] = None,
      expectedExtId: Option[String] = None,
      expectedNote: Option[String] = None,
      js: JsValue
  ): Unit = {
    (js \ "parentObject" \ "objectType").as[String] mustBe parentObjectType
    (js \ "originatedObjectUuid").as[String] mustBe parentObject
    (js \ "isExtracted").as[Boolean] mustBe isExtracted
    (js \ "parentObject" \ "objectId").asOpt[String] mustBe expectedParent
    (js \ "sampleNum").asOpt[Int].getOrElse(0) must be > 0
    (js \ "sampleId").asOpt[String] mustBe expectedSampleId
    (js \ "externalId" \ "value").asOpt[String] mustBe expectedExtId
    (js \ "note").asOpt[String] mustBe expectedNote
  }

  def validateSampleObject(expected: JsValue, actual: JsValue): Unit = {
    val parentObjectType = (expected \ "parentObject" \ "objectType").as[String]
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
  val forNodeUrl =
    (mid: Int) =>
      (nodeId: String) =>
        (cids: Seq[String]) =>
          s"/$mid/node/$nodeId/samples?collectionIds=${cids.mkString(",")}"

  def getAllForTestMuseum = {
    val res =
      wsUrl(forMuseumUrl(mid)).withHeaders(token.asHeader).get().futureValue
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

    "successfully add a few new SampleObjects" in {
      val cd      = DateTime.now.minusWeeks(2)
      val results = createAndSave(cd, Some(parentObject), Some(parentObject))

      forAll(results) { r =>
        r.status mustBe CREATED
      }
    }

    "list all objects for a museum" in {
      val res     = getAllForTestMuseum
      val objects = res.json.as[JsArray].value
      objects.size mustBe 10

      forAll(objects.zipWithIndex) {
        case (obj, index) =>
          validateSampleObject(
            expectedParent = Some(parentObject),
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

    "return 400 when trying to delete a sample object with an invalid UUID" in {
      val objectId = "123"

      val res =
        wsUrl(deleteUrl(mid)(objectId)).withHeaders(token.asHeader).get().futureValue

      res.status mustBe BAD_REQUEST
    }

    "return a list of samples that are placed on a specific nodeId" in {
      val cd             = DateTime.now.minusWeeks(2)
      val archCollection = MuseumCollections.Archeology.uuid.asString
      val destNode       = "6e5b9810-9bbf-464a-a0b9-c27f6095ba0c"
      val origObjId      = "7de44f6e-51f5-4c90-871b-cef8de0ce93d"

      val results = createAndSave(cd, Some(parentObject), Some(origObjId), 5)

      val sampleIds = results.map(_.json.as[String])
      val mvItemsJs = sampleIds.map { id =>
        s"""{
           |  "id": "$id",
           |  "objectType": "sample"
           |}""".stripMargin
      }.mkString(",")
      val mvJs = Json.parse(
        s"""{
           |  "doneBy": "$adminId",
           |  "destination": "$destNode",
           |  "items": [$mvItemsJs]
           |}""".stripMargin
      )

      wsUrl(MoveObjectUrl(mid))
        .withHeaders(token.asHeader)
        .put(mvJs)
        .futureValue
        .status mustBe OK

      // NOW we can do check the service for listing samples on node

      val res =
        wsUrl(forNodeUrl(mid)(destNode)(Seq(archCollection)))
          .withHeaders(token.asHeader)
          .get()
          .futureValue

      res.status mustBe OK

      val resArr = res.json.as[JsArray].value
      resArr.size mustBe 5

      forAll(resArr) { js =>
        (js \ "museumNo").as[String] mustBe "C1"
        (js \ "subNo").as[String] mustBe "13"
        (js \ "term").as[String] mustBe "Fin øks"
        val so = (js \ "sampleObject").as[JsObject]
        sampleIds.contains((so \ "objectId").as[String]) mustBe true
        (so \ "originatedObjectUuid").as[String] mustBe origObjId
        (so \ "parentObject" \ "objectId").as[String] mustBe parentObject
        (so \ "parentObject" \ "objectType").as[String] mustBe "collection"
        (so \ "museumId").as[Int] mustBe mid
      }
    }
  }

}
