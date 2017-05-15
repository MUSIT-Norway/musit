package controllers.analysis

import models.analysis.events.AnalysisExtras.ElementalICPAttributes.ICP_MS
import models.analysis.events.AnalysisExtras.IsotopeAttributes.StrontiumNeodymium
import models.analysis.events.AnalysisExtras.{
  ElementalICPAttributes,
  IsotopeAttributes,
  TomographyAttributes
}
import models.analysis.events.AnalysisExtras.TomographyAttributes.ComputerTomography
import models.analysis.events.EventCategories
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AnalysisControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers
    with AnalysisJsonGenerators
    with AnalysisJsonValidators {

  val mid   = MuseumId(99)
  val token = BearerToken(FakeUsers.testAdminToken)

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
        res.json.as[JsArray].value.size mustBe 45
      }

      "return all event types in an analysis category" in {
        val catId = EventCategories.Dating.id
        val res =
          wsUrl(typeCatUrl(mid)(catId)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 2
      }

      "return all event types related to a museum collection" ignore {
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

      "save a new generic analysis" in {
        val edate = DateTime.now
        val js = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = Seq(dummyObjectId)
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 1 to 2
      }

      "save a new tomography analysis with extra attributes defined" in {
        val edate = DateTime.now
        val jso = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(dummyObjectId),
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
          objects = Seq(dummyObjectId),
          caseNumbers = Some(Seq("abc", "def"))
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 5 to 6
      }

      "return HTTP 400 when using wrong extra attributes for analysis collection" in {
        val edate = DateTime.now
        val jso = createSaveAnalysisCollectionJSON(
          typeId = tomographyTypeId,
          eventDate = Some(edate),
          objects = Seq(dummyObjectId),
          caseNumbers = Some(Seq("abc", "def"))
        )
        val js = appendExtraAttributes(jso, ElementalICPAttributes(ICP_MS))

        saveAnalysis(js).status mustBe BAD_REQUEST
      }

      "get an analysis with a specific Id" in {
        val edate = DateTime.now
        val oid   = ObjectUUID.generate()
        val note  = Some("Foobar")
        val ajs = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = Seq(oid),
          note = note
        )

        saveAnalysis(ajs).status mustBe CREATED // creates ids 7 to 8

        // We can assume the ID is 8 since we've only created 2 cols
        // with 1 analysis each before this.
        val res =
          wsUrl(getAnalysisUrl(mid)(8L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK

        validateAnalysis(
          expectedId = 8L,
          expectedTypeId = liquidChromatography,
          expectedEventDate = Some(edate),
          expectedObject = Some(oid),
          expectedParent = Some(7L),
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
        val js = createSaveAnalysisCollectionJSON(
          eventDate = Some(edate),
          objects = oids,
          note = note,
          caseNumbers = Some(Seq("cn-1", "cn-2"))
        )

        saveAnalysis(js).status mustBe CREATED // creates ids 9 to 13
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
          wsUrl(getAnalysisUrl(mid)(9L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        validateAnalysisCollection(
          expectedId = 9L,
          expectedTypeId = liquidChromatography,
          expectedEventDate = Some(edate),
          expectedNote = Some("Foobar"),
          numChildren = 5,
          res.json
        )
      }

      "get all analyses in an analysis collection/batch" in {
        val res =
          wsUrl(getChildrenUrl(mid)(9L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 5
      }

      "get all analyses related to an object" in {
        val res = wsUrl(getForObjectUrl(mid)(dummyObjectId.asString))
          .withHeaders(token.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 4
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

      "update the result for an analysis" in {
        val js = createGenericResultJSON(
          extRef = Some(Seq("abc", "xyz")),
          comment = Some("A new result was added")
        )

        val res =
          wsUrl(saveResultUrl(mid)(2L)).withHeaders(token.asHeader).put(js).futureValue

        res.status mustBe OK
      }

      "include the new result when fetching the analysis" in {
        val res =
          wsUrl(getAnalysisUrl(mid)(2L)).withHeaders(token.asHeader).get().futureValue

        res.status mustBe OK
        (res.json \ "id").as[Long] mustBe 2L
        (res.json \ "result").asOpt[JsObject] must not be empty
        (res.json \ "result" \ "extRef").as[JsArray].value.size mustBe 2
        (res.json \ "result" \ "extRef" \ 0).as[String] mustBe "abc"
        (res.json \ "result" \ "extRef" \ 1).as[String] mustBe "xyz"
        (res.json \ "result" \ "comment").as[String] mustBe "A new result was added"
      }

    }

  }

}
