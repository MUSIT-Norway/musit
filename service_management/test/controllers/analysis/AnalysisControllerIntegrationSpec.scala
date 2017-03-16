package controllers.analysis

import models.analysis.events.AnalysisResults.GenericResult
import models.analysis.events.{Analysis, AnalysisCollection, Category, EventCategories}
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models.{ActorId, MuseumCollections, MuseumId, ObjectUUID}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AnalysisControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid     = MuseumId(99)
  val token   = BearerToken(FakeUsers.testAdminToken)
  val adminId = ActorId.unsafeFromString(FakeUsers.testAdminId)

  // test data
  val cnRatioTypeId = "fabe6462-ea94-43ce-bf7f-724a4191e114"
  val dummyObjectId = ObjectUUID.generate()

  def createSaveAnalysisJSON(
      eventDate: Option[DateTime],
      oid: ObjectUUID,
      note: Option[String] = None
  ): JsValue = {
    val js1 = Json.obj(
      "analysisTypeId" -> cnRatioTypeId,
      "objectId"       -> oid.asString
    )
    val js2 = note.map(n => js1 ++ Json.obj("note" -> n)).getOrElse(js1)
    eventDate.map { d =>
      js2 ++ Json.obj("eventDate" -> Json.toJson[DateTime](d))
    }.getOrElse(js2)
  }

  def createSaveAnalysisCollectionJSON(
      eventDate: Option[DateTime],
      objects: Seq[ObjectUUID],
      note: Option[String]
  ): JsValue = {
    val js1 = Json.obj("analysisTypeId" -> cnRatioTypeId)
    val js2 = note.map(n => js1 ++ Json.obj("note" -> n)).getOrElse(js1)
    val js3 = eventDate.map { d =>
      js2 ++ Json.obj("eventDate" -> Json.toJson[DateTime](d))
    }.getOrElse(js2)

    js3 ++ Json.obj("objectIds" -> objects.map(_.asString))
  }

  def createGenericResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String]
  ): JsValue = {

    val js1 = Json.obj("type" -> GenericResult.resultType)
    val js2 = comment.map(c => js1 ++ Json.obj("comment" -> c)).getOrElse(js1)
    extRef.map(er => js2 ++ Json.obj("extRef" -> er)).getOrElse(js2)
  }

  def createDatingResultJSON(
      extRef: Option[Seq[String]],
      comment: Option[String],
      age: Option[String]
  ): JsValue = {
    val js1 = Json.obj("type" -> GenericResult.resultType)
    val js2 = comment.map(c => js1 ++ Json.obj("comment" -> c)).getOrElse(js1)
    val js3 = extRef.map(er => js2 ++ Json.obj("extRef" -> er)).getOrElse(js2)
    age.map(a => js3 ++ Json.obj("age" -> a)).getOrElse(js3)
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
    (actual \ "type").as[String] mustBe Analysis.discriminator
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "analysisTypeId").as[String] mustBe expectedTypeId
    (actual \ "eventDate").asOpt[DateTime] mustApproximate expectedEventDate
    (actual \ "objectId").asOpt[String] mustBe expectedObject.map(_.asString)
    (actual \ "partOf").asOpt[Long] mustBe expectedParent
    (actual \ "note").asOpt[String] mustBe expectedNote
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
  }

  def validateAnalysisCollection(
      expectedId: Long,
      expectedTypeId: String,
      expectedEventDate: Option[DateTime],
      expectedNote: Option[String],
      numChildren: Int,
      actual: JsValue
  ) = {
    (actual \ "type").as[String] mustBe AnalysisCollection.discriminator
    (actual \ "id").as[Long] mustBe expectedId
    (actual \ "analysisTypeId").as[String] mustBe expectedTypeId
    (actual \ "eventDate").asOpt[DateTime] mustApproximate expectedEventDate
    (actual \ "registeredBy").asOpt[String] must not be empty
    (actual \ "registeredDate").asOpt[DateTime] must not be empty
    forAll(0 until numChildren) { index =>
      (actual \ "events" \ index \ "type").as[String] mustBe Analysis.discriminator
      (actual \ "events" \ index \ "partOf").as[Long] mustBe expectedId
      (actual \ "events" \ index \ "note").asOpt[String] mustBe expectedNote
    }
  }

  val baseUrl = (mid: Int) => s"/$mid/analyses"

  val typesUrl   = (mid: Int) => s"${baseUrl(mid)}/types"
  val typeIdUrl  = (mid: Int) => (id: Long) => s"${typesUrl(mid)}/$id"
  val typeCatUrl = (mid: Int) => (id: Int) => s"${typesUrl(mid)}/categories/$id"
  val typeColUrl = (mid: Int) => (id: String) => s"${typesUrl(mid)}/musemcollections/$id"

  val addAnalysisUrl  = baseUrl
  val getAnalysisUrl  = (mid: Int) => (id: Long) => s"${baseUrl(mid)}/$id"
  val getChildrenUrl  = (mid: Int) => (id: Long) => s"${getAnalysisUrl(mid)(id)}/children"
  val saveResultUrl   = (mid: Int) => (id: Long) => s"${getAnalysisUrl(mid)(id)}/results"
  val getForObjectUrl = (mid: Int) => (oid: String) => s"${baseUrl(mid)}/objects/$oid"

  def saveAnalysis(ajs: JsValue): WSResponse = {
    wsUrl(addAnalysisUrl(mid)).withHeaders(token.asHeader).post(ajs).futureValue
  }

  "Using the analysis controller" when {

    "fetching analysis types" should {

      "return all event types" in {
        val res = wsUrl(typesUrl(mid)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 107
      }

      "return all event types in an analysis category" in {
        val catId = EventCategories.Dating.id
        val res =
          wsUrl(typeCatUrl(mid)(catId)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 8
      }

      "return all event types related to a museum collection" in {
        val cid = MuseumCollections.Entomology.uuid
        val res = wsUrl(typeColUrl(mid)(cid.asString))
          .withHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 28
      }

    }

    "working with analysis" should {

      "save new analysis data" in {
        val edate = DateTime.now
        val ajs   = createSaveAnalysisJSON(Some(edate), dummyObjectId)

        saveAnalysis(ajs).status mustBe CREATED
      }

      "get an analysis with a specific Id" in {
        val edate = DateTime.now
        val oid   = ObjectUUID.generate()
        val note  = Some("Foobar")
        val ajs   = createSaveAnalysisJSON(Some(edate), oid, note)

        saveAnalysis(ajs).status mustBe CREATED

        // We can assume the ID is 2 since we've only created 1 analysis before this
        val res =
          wsUrl(getAnalysisUrl(mid)(2L)).withHeaders(token.asHeader).get().futureValue

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
        wsUrl(getAnalysisUrl(mid)(100L))
          .withHeaders(token.asHeader)
          .get()
          .futureValue
          .status mustBe NOT_FOUND
      }

      "save a new collection/batch of analyses" in {
        val edate = DateTime.now
          .withYear(2017)
          .withMonthOfYear(3)
          .withDayOfMonth(12)
          .withHourOfDay(11)
          .withMinuteOfHour(23)
          .withSecondOfMinute(44)

        val note = Some("Foobar")
        val oids = (1 to 4).map(_ => ObjectUUID.generate()) :+ dummyObjectId
        val js   = createSaveAnalysisCollectionJSON(Some(edate), oids, note)

        saveAnalysis(js).status mustBe CREATED
      }

      "return an analysis collection with a specific ID" in {
        val edate = DateTime.now
          .withYear(2017)
          .withMonthOfYear(3)
          .withDayOfMonth(12)
          .withHourOfDay(11)
          .withMinuteOfHour(23)
          .withSecondOfMinute(44)

        val res =
          wsUrl(getAnalysisUrl(mid)(3L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        validateAnalysisCollection(
          expectedId = 3L,
          expectedTypeId = cnRatioTypeId,
          expectedEventDate = Some(edate),
          expectedNote = Some("Foobar"),
          numChildren = 5,
          res.json
        )
      }

      "get all analyses in an analysis collection/batch" in {
        // This test _assumes_ that we've only added 3 events previously. Where
        // the 3rd event was the above AnalysisCollection. Meaning the event ID
        // given to the AnalysisCollection should be 3L. Children should get ID's
        // from 4L to 8L.
        val res =
          wsUrl(getChildrenUrl(mid)(3L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 5
      }

      "get all analyses related to an object" in {
        val res = wsUrl(getForObjectUrl(mid)(dummyObjectId.asString))
          .withHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 2
      }

      "save a generic result for an analysis" in {
        val js = createGenericResultJSON(
          extRef = Some(Seq("Ref1", "ref2")),
          comment = Some("See references")
        )

        val res =
          wsUrl(saveResultUrl(mid)(2L)).withHeaders(token.asHeader).post(js).futureValue

        res.status mustBe CREATED
      }

      "include the saved result when fetching the analysis" in {
        // We can assume the ID is 2 since we've only created 1 analysis before this
        val res =
          wsUrl(getAnalysisUrl(mid)(2L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        (res.json \ "id").as[Long] mustBe 2L
        (res.json \ "result").asOpt[JsObject] must not be empty
        (res.json \ "result" \ "extRef").as[JsArray].value.size mustBe 2
        (res.json \ "result" \ "extRef" \ 0).as[String] mustBe "Ref1"
        (res.json \ "result" \ "extRef" \ 1).as[String] mustBe "ref2"
        (res.json \ "result" \ "comment").as[String] mustBe "See references"
      }

      "save a new result for an analysis" in {
        //        val js = createGenericResultJSON(
        //          extRef = Some(Seq("abc", "xyz")),
        //          comment = Some("A new result was added")
        //        )
        //
        //        val res = wsUrl(saveResultUrl(2L))
        //          .withHeaders(token.asHeader)
        //          .post(js)
        //          .futureValue
        //
        //        res.status mustBe CREATED

        // Test is pending until the result handling is sorted out
        pending
      }

      "include the new result when fetching the analysis" in {
        // We can assume the ID is 2 since we've only created 1 analysis before this
        //        val res = wsUrl(getAnalysisUrl(2L))
        //          .withHeaders(token.asHeader)
        //          .get()
        //          .futureValue
        //
        //        res.status mustBe OK
        //        (res.json \ "id").as[Long] mustBe 2L
        //        (res.json \ "result").asOpt[JsObject] must not be empty
        //        (res.json \ "result" \ "extRef").as[JsArray].value.size mustBe 2
        //        (res.json \ "result" \ "extRef" \ 0).as[String] mustBe "abc"
        //        (res.json \ "result" \ "extRef" \ 1).as[String] mustBe "xyz"
        //        (res.json \ "result" \ "comment").as[String] mustBe "A new result was added"

        // Test is pending until the result handling is sorted out
        pending
      }

    }

  }

}
