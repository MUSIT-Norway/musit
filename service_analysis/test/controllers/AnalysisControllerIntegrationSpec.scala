package controllers

import models.events.{AnalysisCollection, Category, EventCategories}
import no.uio.musit.models.{ActorId, MuseumCollections, MuseumId, ObjectUUID}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AnalysisControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val mid = MuseumId(99)
  val adminToken = BearerToken(FakeUsers.testAdminToken)
  val adminId = ActorId.unsafeFromString(FakeUsers.testAdminId)

  // test data
  val cnRatioTypeId = "fabe6462-ea94-43ce-bf7f-724a4191e114"

  def createAnalysisJSON(
    eventDate: Option[DateTime],
    oid: Option[ObjectUUID],
    note: Option[String] = None
  ): JsValue = {
    val js1 = Json.obj("analysisTypeId" -> cnRatioTypeId)
    val js2 = oid.map(i => js1 ++ Json.obj("objectId" -> i.asString)).getOrElse(js1)
    val js3 = note.map(n => js2 ++ Json.obj("note" -> n)).getOrElse(js2)
    eventDate.map(d => js3 ++ Json.obj("eventDate" -> Json.toJson(d))).getOrElse(js3)
  }

  def createSaveAnalysisCollectionJSON(
    eventDate: Option[DateTime],
    objects: Seq[ObjectUUID],
    note: Option[String] = None
  ): JsValue = {
    val js1 = Json.obj("analysisTypeId" -> cnRatioTypeId)
    val js2 = note.map(n => js1 ++ Json.obj("note" -> n)).getOrElse(js1)
    val js3 = eventDate.map { d =>
      js2 ++ Json.obj("eventDate" -> Json.toJson(d))
    }.getOrElse(js2)

    js3 ++ Json.obj("objectsIds" -> Json.arr(objects.map(_.asString)))
  }

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
    expectedTypeId: String,
    expectedEventDate: Option[DateTime],
    expectedObject: Option[ObjectUUID],
    expectedParent: Option[Long],
    expectedNote: Option[String],
    actual: JsValue
  ) = {
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "analysisTypeId").as[String] mustBe expectedTypeId
    (actual \ "eventDate").asOpt[DateTime] mustBe expectedEventDate
    (actual \ "objectId").asOpt[String] mustBe expectedObject.map(_.asString)
    (actual \ "partOf").asOpt[Long] mustBe expectedParent
    (actual \ "note").asOpt[String] mustBe expectedNote
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
  }

  val baseUrl = "/analyses"

  val typesUrl = s"$baseUrl/types"
  val typeIdUrl = (id: Long) => s"$typesUrl/$id"
  val typeCatUrl = (id: Int) => s"$typesUrl/categories/$id"
  val typeColUrl = (id: String) => s"$typesUrl/musemcollections/$id"

  val addAnalysisUrl = baseUrl
  val getAnalysisUrl = (id: Long) => s"$baseUrl/$id"
  val getChildrenUrl = (id: Long) => s"${getAnalysisUrl(id)}/children"
  val getForObjectUrl = (oid: String) => s"$baseUrl/objects/$oid"

  def saveAnalysis(ajs: JsValue): WSResponse = {
    wsUrl(addAnalysisUrl)
      .withHeaders(adminToken.asHeader)
      .post(ajs)
      .futureValue
  }

  "Running the analysis service" when {

    "fetching analysis types" should {

      "return all event types" in {
        val res = wsUrl(typesUrl)
          .withHeaders(adminToken.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 107
      }

      "return all event types in an analysis category" in {
        val catId = EventCategories.Dating.id
        val res = wsUrl(typeCatUrl(catId))
          .withHeaders(adminToken.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 8
      }

      "return all event types related to a museum collection" in {
        val cid = MuseumCollections.Entomology.uuid
        val res = wsUrl(typeColUrl(cid.asString))
          .withHeaders(adminToken.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 28
      }

    }

    "working with analysis" should {

      "save new analysis data" in {
        val edate = DateTime.now
        val oid = ObjectUUID.generate()
        val ajs = createAnalysisJSON(Some(edate), Some(oid))

        saveAnalysis(ajs).status mustBe CREATED
      }

      "get an analysis with a given Id" in {
        val edate = DateTime.now
        val oid = ObjectUUID.generate()
        val note = Some("Foobar")
        val ajs = createAnalysisJSON(Some(edate), Some(oid), note)

        saveAnalysis(ajs).status mustBe CREATED

        // We can assume the ID is 2 since we've only created 1 analysis before this
        val res = wsUrl(getAnalysisUrl(2L))
          .withHeaders(adminToken.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        validateAnalysis(
          expectedId = 2L,
          expectedTypeId = cnRatioTypeId,
          expectedEventDate = Some(edate),
          expectedObject = Some(oid),
          expectedParent = None,
          expectedNote = note,
          res.json
        )
      }

      "return 404 NotFound if the ID can't be found" in {
        wsUrl(getAnalysisUrl(100L))
          .withHeaders(adminToken.asHeader)
          .get()
          .futureValue
          .status mustBe NOT_FOUND
      }

      "save a new collection/batch of analyses" in {
        val edate = DateTime.now
        val note = Some("Foobar")
        val oids = (1 to 5).map(_ => ObjectUUID.generate())
        val js = createSaveAnalysisCollectionJSON(Some(edate), oids, note)

        saveAnalysis(js).status mustBe CREATED
      }

      "get all analyses in an analysis collection/batch" in {
        pending
      }

      "get all analyses related to an object" in {
        pending
      }

      "save a result for an analysis" in {
        pending
      }

      "saving a new result for an analysis" in {
        pending
      }

    }

  }

}
