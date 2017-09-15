package controllers.analysis

import controllers.storage.MoveObjectUrl
import models.analysis.SampleStatuses.{Intact, SampleStatus}
import no.uio.musit.formatters.DateTimeFormatters._
import no.uio.musit.models.ObjectTypes.{
  CollectionObjectType,
  ObjectType,
  SampleObjectType
}
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
  val tokenRead    = BearerToken(FakeUsers.testReadToken)
  val tokenTest    = BearerToken(FakeUsers.testUserToken)
  val adminId      = FakeUsers.testAdminId
  val dummyActorId = ActorId.generate().asString

  val responsibleActor = ActorId.generate().asString
  val parentObject     = "9dfa0946-3b71-4382-888f-3f924ff48a77"

  // scalastyle:off
  def createSaveJSON(
      originatingObject: String,
      maybeId: Option[String] = None,
      maybeParent: Option[String] = None,
      parentObjectType: ObjectType,
      isExtracted: Boolean = true,
      status: SampleStatus = Intact,
      doneBy: String,
      doneDate: DateTime,
      maybeSampleId: Option[String],
      maybeExtId: Option[String],
      maybeNote: Option[String]
  ) = {
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
      "originatedObjectUuid" -> originatingObject
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
      origObject: String,
      maybeParent: Option[String],
      t: BearerToken = token,
      parentObjectType: ObjectType = CollectionObjectType,
      numToCreate: Int = 10
  ): Seq[WSResponse] = {
    (1 to numToCreate).map { index =>
      val js = createSaveJSON(
        originatingObject = origObject,
        maybeParent = maybeParent,
        parentObjectType = parentObjectType,
        doneBy = adminId,
        doneDate = cd,
        maybeSampleId = Some(s"sample$index"),
        maybeExtId = Some(s"ext$index"),
        maybeNote = Some("This is a sample note")
      )
      wsUrl(addUrl(mid)).withHttpHeaders(t.asHeader).post(js).futureValue
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
  val forOrigUrl   = (mid: Int) => (oid: String) => s"${getUrl(mid)(oid)}/all"
  val deleteUrl    = (mid: Int) => (oid: String) => s"${baseUrl(mid)}/$oid"
  val forNodeUrl =
    (mid: Int) =>
      (nodeId: String) =>
        (cids: Seq[String]) =>
          s"/$mid/node/$nodeId/samples?collectionIds=${cids.mkString(",")}"

  def getAllForTestMuseum = {
    val res =
      wsUrl(forMuseumUrl(mid)).withHttpHeaders(token.asHeader).get().futureValue
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

  val addedSampleIds = Seq.newBuilder[String]

  "Invoking the sample object controller API" should {

    "successfully add a few new SampleObjects" in {
      val cd      = DateTime.now.minusWeeks(2)
      val results = createAndSave(cd, parentObject, Some(parentObject))

      forAll(results) { r =>
        r.status mustBe CREATED
        addedSampleIds += r.json.as[String]
      }
    }

    "return 400 BAD_REQUEST if originating object isn't a collection object" in {
      val cd             = DateTime.now.minusWeeks(2)
      val invalidOrigObj = ObjectUUID.generate().asString
      val results =
        createAndSave(cd, invalidOrigObj, Some(invalidOrigObj), numToCreate = 1)
      results.headOption.map(_.status) mustBe Some(BAD_REQUEST)
    }

    "list all objects for a museum" in {
      val res     = getAllForTestMuseum
      val objects = res.json.as[JsArray].value
      objects.size mustBe 11

      // Check all samples _except_ the sample that was inserted in the
      // test/resources/.../1.sql file. A bit naive in that it will fail if
      // current ordering changes.
      forAll(objects.tail.zipWithIndex) {
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

    "return a list of derived samples for a specific originating object" in {
      val cd   = DateTime.now.minusWeeks(1)
      val orig = "5a928d42-05a6-44db-adef-c6dfe588f016"
      val cs1 = createAndSave(cd, orig, Some(orig), numToCreate = 2).map { r =>
        r.status mustBe CREATED
        val sid = r.json.as[String]
        addedSampleIds += sid
        sid
      }
      val (s1, s2) = (cs1.head, cs1.last)

      val cs2 = createAndSave(cd, orig, Some(s2), token, SampleObjectType, 2).map { r =>
        r.status mustBe CREATED
        val sid = r.json.as[String]
        addedSampleIds += sid
        sid
      }
      val (s3, s4) = (cs2.head, cs2.last)

      val res =
        wsUrl(forOrigUrl(mid)(orig)).withHttpHeaders(tokenRead.asHeader).get().futureValue

      res.status mustBe OK
      val objects = res.json.as[JsArray].value
      objects.size mustBe 4

      forAll(objects) { js =>
        (js \ "originatedObjectUuid").as[String] mustBe orig
      }
    }

    "return a list of derived samples for a specific parent" in {
      val cd   = DateTime.now.minusWeeks(1)
      val orig = "baec467b-2fd2-48d3-9fe1-6f0ea30a3497"
      val cs1 = createAndSave(cd, orig, Some(orig), numToCreate = 2).map { r =>
        r.status mustBe CREATED
        val sid = r.json.as[String]
        addedSampleIds += sid
        sid
      }
      val (s1, s2) = (cs1.head, cs1.last)

      val cs2 = createAndSave(cd, orig, Some(s2), token, SampleObjectType, 2).map { r =>
        r.status mustBe CREATED
        val sid = r.json.as[String]
        addedSampleIds += sid
        sid
      }
      val (s3, s4) = (cs2.head, cs2.last)

      val res =
        wsUrl(childrenUrl(mid)(s2)).withHttpHeaders(tokenRead.asHeader).get().futureValue

      res.status mustBe OK
      val objects = res.json.as[JsArray].value
      objects.size mustBe 2

      forAll(objects) { js =>
        (js \ "originatedObjectUuid").as[String] mustBe orig
        (js \ "parentObject" \ "objectId").as[String] mustBe s2
        (js \ "parentObject" \ "objectType").as[String] mustBe SampleObjectType.name
      }
    }

    "return the sample object with the given ID" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(getUrl(mid)(objectId)).withHttpHeaders(tokenRead.asHeader).get().futureValue

      res.status mustBe OK

      res.json mustBe expJs
    }

    "update a specific sample object" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val ujs = updateJson[String](expJs, __ \ "note", "Updated note")

      val res =
        wsUrl(updateUrl(mid)(objectId))
          .withHttpHeaders(token.asHeader)
          .put(ujs)
          .futureValue

      res.status mustBe OK
      validateSampleObject(ujs, res.json)
    }

    "delete a specific sample object" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(deleteUrl(mid)(objectId))
          .withHttpHeaders(token.asHeader)
          .delete()
          .futureValue

      res.status mustBe OK
    }

    "return 404 when trying to delete a non-existing sample objectid" in {
      val objectId = "123e4567-e89b-12d3-a456-426655440000"

      val res =
        wsUrl(deleteUrl(mid)(objectId))
          .withHttpHeaders(token.asHeader)
          .delete()
          .futureValue

      res.status mustBe NOT_FOUND
    }

    "return 400 when trying to delete a sample object with an invalid UUID" in {
      val objectId = "123"

      val res =
        wsUrl(deleteUrl(mid)(objectId))
          .withHttpHeaders(token.asHeader)
          .delete()
          .futureValue

      res.status mustBe BAD_REQUEST
    }

    "return a list of samples that are placed on a specific nodeId" in {
      val cd             = DateTime.now.minusWeeks(2)
      val archCollection = MuseumCollections.Archeology.uuid.asString
      val destNode       = "6e5b9810-9bbf-464a-a0b9-c27f6095ba0c"
      val origObjId      = "7de44f6e-51f5-4c90-871b-cef8de0ce93d"

      val results = createAndSave(cd, origObjId, Some(origObjId), numToCreate = 5)

      forAll(results) { r =>
        r.status mustBe CREATED
        addedSampleIds += r.json.as[String]
      }

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
        .withHttpHeaders(token.asHeader)
        .put(mvJs)
        .futureValue
        .status mustBe OK

      // NOW we can do check the service for listing samples on node

      val res =
        wsUrl(forNodeUrl(mid)(destNode)(Seq(archCollection)))
          .withHttpHeaders(token.asHeader)
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
        (so \ "parentObject" \ "objectId").as[String] mustBe origObjId
        (so \ "parentObject" \ "objectType").as[String] mustBe "collection"
        (so \ "museumId").as[Int] mustBe mid
      }
    }

    "Return 403 Forbidden when trying to add a few new SampleObjects without permission" in {
      val cd      = DateTime.now.minusWeeks(2)
      val results = createAndSave(cd, parentObject, Some(parentObject), tokenTest)

      forAll(results) { r =>
        r.status mustBe FORBIDDEN
      }
    }

    "Return 403 Forbidden when trying to delete a specific sample object without permission" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(deleteUrl(mid)(objectId))
          .withHttpHeaders(tokenRead.asHeader)
          .delete()
          .futureValue

      res.status mustBe FORBIDDEN
    }

    "Return 403 Forbidden when trying to update a specific sample object without permission" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val ujs = updateJson[String](expJs, __ \ "note", "Updated note")

      val res =
        wsUrl(updateUrl(mid)(objectId))
          .withHttpHeaders(tokenRead.asHeader)
          .put(ujs)
          .futureValue

      res.status mustBe FORBIDDEN
    }

    "Return 403 Forbidden when trying to return the sample object with the given ID without permission" in {
      val all = getAllForTestMuseum

      val expJs    = (all.json \ 2).as[JsObject]
      val objectId = (expJs \ "objectId").as[String]

      val res =
        wsUrl(getUrl(mid)(objectId)).withHttpHeaders(tokenTest.asHeader).get().futureValue

      res.status mustBe FORBIDDEN

    }
  }

}
