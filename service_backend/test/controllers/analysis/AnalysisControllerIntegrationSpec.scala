package controllers.analysis

import models.analysis.events.AnalysisExtras.ElementalICPAttributes.ICP_MS
import models.analysis.events.AnalysisExtras.TomographyAttributes.{
  ComputerTomography,
  NeutronTomography,
  StructuredLight3DScan
}
import models.analysis.events.AnalysisExtras.{
  ElementalICPAttributes,
  TomographyAttributes
}
import models.analysis.events.EventCategories
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models.MuseumCollections.Archeology
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AnalysisControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers
    with AnalysisJsonGenerators
    with AnalysisJsonValidators {

  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenRead = BearerToken(FakeUsers.testReadToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val baseUrl = (mid: Int) => s"/$mid/analyses"

  val typesUrl      = (mid: Int) => s"${baseUrl(mid)}/types"
  val categoriesUrl = (mid: Int) => s"${baseUrl(mid)}/categories"
  val typeIdUrl     = (mid: Int) => (id: Long) => s"${typesUrl(mid)}/$id"
  val typeCatUrl    = (mid: Int) => (id: Int) => s"${typesUrl(mid)}?categoryId=$id"
  val typeColUrl    = (mid: Int) => (id: String) => s"${typesUrl(mid)}?collectionIds=$id"

  val addAnalysisUrl  = baseUrl
  val getAnalysesUrl  = baseUrl
  val getAnalysisUrl  = (mid: Int) => (id: Long) => s"${baseUrl(mid)}/$id"
  val getChildrenUrl  = (mid: Int) => (id: Long) => s"${getAnalysisUrl(mid)(id)}/children"
  val saveResultUrl   = (mid: Int) => (id: Long) => s"${getAnalysisUrl(mid)(id)}/results"
  val getForObjectUrl = (mid: Int) => (oid: String) => s"${baseUrl(mid)}/objects/$oid"

  def saveAnalysis(ajs: JsValue, t: BearerToken = token): WSResponse = {
    wsUrl(addAnalysisUrl(mid)).withHttpHeaders(t.asHeader).post(ajs).futureValue
  }

  "Using the analysis controller" when {

    "fetching analysis types" should {

      "return all event types" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 46
      }

      "return all event categories" in {
        val res =
          wsUrl(categoriesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 13
        (res.json \ 0 \ "name").as[String] mustBe "Chemical"
        (res.json \ 0 \ "id").as[Int] mustBe 1
      }

      "return all event types in an analysis category" in {
        val catId = EventCategories.Dating.id
        val res =
          wsUrl(typeCatUrl(mid)(catId))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 3
      }

      "return all types in a category where some have extra description attributes" in {
        val catId = EventCategories.Image.id
        val res =
          wsUrl(typeCatUrl(mid)(catId))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        val arr = res.json.as[JsArray].value
        arr.size mustBe 4

        val js = res.json
        (js \ 0 \ "id").as[Int] mustBe 5
        (js \ 0 \ "category").as[Int] mustBe catId
        (js \ 0 \ "extraDescriptionType").as[String] mustBe "MicroscopyAttributes"

        val extraAttrs = (js \ 0 \ "extraDescriptionAttributes").as[JsArray]
        (extraAttrs \ 0 \ "attributeKey").as[String] mustBe "method"
        (extraAttrs \ 0 \ "attributeType").as[String] mustBe "Int"

        val allowedValues = (extraAttrs \ 0 \ "allowedValues").as[JsArray].value
        allowedValues.size mustBe 4

        val first = allowedValues.head
        (first \ "id").as[Int] mustBe 1
        (first \ "enLabel").as[String] mustBe "Light/polarization microscopy"
        (first \ "noLabel").as[String] mustBe "Lys-/polarisasjonsmikroskopi"
      }

      "return measurement type with extra result type" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        val arr = res.json.as[JsArray].value
        arr.size mustBe 46

        val js = res.json

        (js \ 39 \ "extraResultType").as[String] mustBe "MeasurementResult"
      }

      "return all event types related to a museum collection" ignore {
        val cid = MuseumCollections.Entomology.uuid
        val res = wsUrl(typeColUrl(mid)(cid.asString))
          .withHttpHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 28
      }

      /* "return 403 FORBIDDEN if not permission to module" in {
        val catId = EventCategories.Dating.id
        val res =
          wsUrl(typeCatUrl(mid)(catId))
            .withHttpHeaders(tokenTest.asHeader)
            .get()
            .futureValue

        res.status mustBe FORBIDDEN
      }*/

    }

    "working with analysis" should {

      "save a new generic analysis" in {
        val edate = DateTime.now
        val js = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = Seq(testObjectUUID)
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 1 to 2
      }

      "save a new tomography analysis with extra attributes defined" in {
        val edate = DateTime.now
        val jso = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(testObjectUUID),
          caseNumbers = Some(Seq("abc", "def"))
        )
        val js = appendExtraAttributes(jso, TomographyAttributes(ComputerTomography))

        saveAnalysis(js).status mustBe CREATED // creates ids 3 to 4
      }

      "save a new tomography analysis without extra attributes defined" in {
        val edate = DateTime.now
        val js = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(testObjectUUID),
          caseNumbers = Some(Seq("abc", "def"))
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 5 to 6
      }

      "successfully update an analysis" in {
        val edate = DateTime.now
        val jso = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(testObjectUUID),
          caseNumbers = Some(Seq("abc", "def")),
          orgId = Some(OrgId(312))
        )
        val js = appendExtraAttributes(jso, TomographyAttributes(StructuredLight3DScan))

        saveAnalysis(js).status mustBe CREATED // creates ids 7 to 8
        val ares =
          wsUrl(getAnalysisUrl(mid)(7L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        ares.status mustBe OK
        (ares.json \ "orgId").as[Int] mustBe 312
        val updJso = js ++ Json.obj(
          "reason"         -> "There's a reason now",
          "status"         -> 2,
          "analysisTypeId" -> 12, // Should not be modified by the server.
          "orgId"          -> 317,
          "restriction" -> Json.obj(
            "requester"       -> adminId,
            "reason"          -> "secret",
            "expirationDate"  -> time.dateTimeNow.plusDays(20),
            "cancelledReason" -> "Illegal restriction",
            "cancelledBy"     -> adminId
          )
        )

        val updJson =
          appendExtraAttributes(updJso, TomographyAttributes(NeutronTomography))

        val updRes = wsUrl(getAnalysisUrl(mid)(7L))
          .withHttpHeaders(token.asHeader)
          .put(updJson)
          .futureValue
        updRes.status mustBe OK

        (updRes.json \ "reason").as[String] mustBe "There's a reason now"
        (updRes.json \ "status").as[Int] mustBe 2
        (updRes.json \ "id").as[Int] mustBe 7
        (updRes.json \ "analysisTypeId").as[Int] mustBe tomographyTypeId
        (updRes.json \ "orgId").as[Int] mustBe 317
        (updRes.json \ "extraAttributes" \ "method").as[Int] mustBe NeutronTomography.id
        (updRes.json \ "restriction" \ "cancelledReason")
          .as[String] mustBe "Illegal restriction"
        (updRes.json \ "restriction" \ "cancelledStamp" \ "user")
          .as[String] mustBe adminId.asString
      }

      "return HTTP 400 when using wrong extra attributes for analysis collection" in {
        val edate = DateTime.now
        val jso = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(testObjectUUID),
          caseNumbers = Some(Seq("abc", "def"))
        )
        val js = appendExtraAttributes(jso, ElementalICPAttributes(ICP_MS))

        saveAnalysis(js).status mustBe BAD_REQUEST
      }

      "get an analysis with a specific Id" in {
        val edate = DateTime.now
        val oid   = validObjectUUIDs(8)
        val note  = Some("Foobar")
        val ajs = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = Seq(oid),
          note = note
        )

        saveAnalysis(ajs).status mustBe CREATED // creates ids 9 to 10

        // We can assume the ID is 8 since we've only created 2 cols
        // with 1 analysis each before this.
        val res =
          wsUrl(getAnalysisUrl(mid)(10L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK

        validateAnalysis(
          expectedId = 10L,
          expectedTypeId = liquidChromatography,
          expectedEventDate = Some(edate),
          expectedObject = Some(oid),
          expectedParent = Some(9L),
          expectedNote = note,
          res.json
        )
      }

      "return 404 NotFound if the ID can't be found" in {
        wsUrl(getAnalysisUrl(mid)(100L))
          .withHttpHeaders(tokenRead.asHeader)
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
        val oids = (2 to 5).map(i => validObjectUUIDs(i)) :+ testObjectUUID
        val js = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = oids,
          note = note,
          caseNumbers = Some(Seq("cn-1", "cn-2"))
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 11 to 15
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
          wsUrl(getAnalysisUrl(mid)(11L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        validateAnalysisCollection(
          expectedId = 11L,
          expectedTypeId = liquidChromatography,
          expectedEventDate = Some(edate),
          expectedNote = Some("Foobar"),
          numChildren = 5,
          res.json
        )
      }

      "get all analyses in an analysis collection/batch" in {
        val res =
          wsUrl(getChildrenUrl(mid)(11L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 5
      }

      "get all analyses related to an object" in {
        val res = wsUrl(getForObjectUrl(mid)(testObjectUUID.asString))
          .withHttpHeaders(tokenRead.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 5
      }

      "save a generic result for an analysis" in {
        val js = createGenericResultJSON(
          extRef = Some(Seq("Ref1", "ref2")),
          comment = Some("See references")
        )

        val res =
          wsUrl(saveResultUrl(mid)(2L))
            .withHttpHeaders(token.asHeader)
            .post(js)
            .futureValue

        res.status mustBe CREATED
      }

      "include the saved result when fetching the analysis" in {
        val res =
          wsUrl(getAnalysisUrl(mid)(2L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        (res.json \ "id").as[Long] mustBe 2L
        (res.json \ "result").asOpt[JsObject] must not be empty
        (res.json \ "result" \ "extRef").as[JsArray].value.size mustBe 2
        (res.json \ "result" \ "extRef" \ 0).as[String] mustBe "Ref1"
        (res.json \ "result" \ "extRef" \ 1).as[String] mustBe "ref2"
        (res.json \ "result" \ "comment").as[String] mustBe "See references"
      }

      "update the result for an analysis" in {
        val js = createGenericResultJSON(
          extRef = Some(Seq("abc", "xyz")),
          comment = Some("A new result was added")
        )

        val res =
          wsUrl(saveResultUrl(mid)(2L))
            .withHttpHeaders(token.asHeader)
            .put(js)
            .futureValue

        res.status mustBe OK

        val resNotPermission =
          wsUrl(saveResultUrl(mid)(2L))
            .withHttpHeaders(tokenTest.asHeader)
            .put(js)
            .futureValue

        resNotPermission.status mustBe FORBIDDEN
      }

      "include the new result when fetching the analysis" in {
        val res =
          wsUrl(getAnalysisUrl(mid)(2L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue

        res.status mustBe OK
        (res.json \ "id").as[Long] mustBe 2L
        (res.json \ "result").asOpt[JsObject] must not be empty
        (res.json \ "result" \ "extRef").as[JsArray].value.size mustBe 2
        (res.json \ "result" \ "extRef" \ 0).as[String] mustBe "abc"
        (res.json \ "result" \ "extRef" \ 1).as[String] mustBe "xyz"
        (res.json \ "result" \ "comment").as[String] mustBe "A new result was added"
      }

      "only return analyses related to a collection in a museum" in {
        val res =
          wsUrl(getAnalysesUrl(mid))
            .withHttpHeaders(tokenRead.asHeader)
            .withQueryStringParameters("collectionIds" -> Archeology.uuid.asString)
            .get()
            .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.length mustBe 6
      }

      "get an analysis with orgId" in {
        val al =
          wsUrl(getAnalysisUrl(mid)(7L))
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue
        al.status mustBe OK
        (al.json \ "orgId").asOpt[Int] mustBe Some(317)
      }

      "return 403 if the user has wrong permission" in {
        wsUrl(getAnalysisUrl(mid)(7L))
          .withHttpHeaders(tokenTest.asHeader)
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

      "return 403 forbidden when trying to save a new generic analysis without permission to module" in {
        val edate = DateTime.now
        val js = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = Seq(testObjectUUID)
        )

        saveAnalysis(js, tokenTest).status mustBe FORBIDDEN // creates ids 1 to 2
      }

    }
  }
}
