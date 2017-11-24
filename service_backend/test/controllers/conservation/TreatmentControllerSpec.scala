package controllers.treatment

import controllers.conservation.{ConservationJsonGenerators, ConservationJsonValidators}
import models.conservation.events.Treatment
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

//Hint, to run only this test, type:
//test-only controllers.treatment.TreatmentControllerSpec

class TreatmentControllerSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers
    with ConservationJsonGenerators
    with ConservationJsonValidators {
  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenRead = BearerToken(FakeUsers.testReadToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val baseUrl      = (mid: Int) => s"/$mid/conservation"
  val baseEventUrl = (mid: Int) => s"/$mid/conservation/events"

  val addTreatmentUrl      = baseEventUrl
  val getTreatmentUrl      = baseEventUrl
  val getTreatmentByIdUrl  = (mid: Int) => (id: Long) => s"${baseEventUrl(mid)}/$id"
  val putTreatmentByIdUrl  = (mid: Int) => (id: Long) => s"${baseEventUrl(mid)}/$id"
  val getTreatmentMaterial = s"/conservation/treatmentMaterials"
  val getKeywordMaterial   = s"/conservation/treatmentKeywords"

  def addDummyTreatment(t: BearerToken = token): WSResponse = {
    val js =
      dummyEventJSON(
        treatmentEventTypeId,
        Some(dateTimeNow),
        Some("testKommentar"),
        Some("777"),
        Some(testAffectedThings)
      )
    wsUrl(addTreatmentUrl(mid)).withHttpHeaders(t.asHeader).post(js).futureValue
  }

  implicit val minReads = Treatment.reads

  def getTreatment(
      mid: MuseumId,
      eventId: EventId,
      t: BearerToken = token
  ): Treatment = {
    val cp = wsUrl(getTreatmentByIdUrl(mid)(eventId))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
    cp.json.validate[Treatment].get
  }

  def putTreatmentResponse(
      mid: MuseumId,
      eventId: EventId,
      json: JsObject,
      t: BearerToken = token
  ): WSResponse = {
    wsUrl(putTreatmentByIdUrl(mid)(eventId))
      .withHttpHeaders(t.asHeader)
      .put(json)
      .futureValue
  }

  def putTreatment(
      mid: MuseumId,
      eventId: EventId,
      json: JsObject,
      t: BearerToken = token
  ): Treatment = {
    val cp = putTreatmentResponse(mid, eventId, json, t)
    cp.json.validate[Treatment].get
  }

  "Using the treatment controller" when {

    "working with treatment" should {

      "add a new treatment" in {

        val res = addDummyTreatment()

        res.status mustBe CREATED // creates ids 1 to 2
        (res.json \ "id").as[Int] mustBe 1
      }
      "get a treatment by it's ID" in {
        val eventId = 1L
        val res1    = getTreatment(mid, eventId)

        res1.eventTypeId.underlying mustBe treatmentEventTypeId
        res1.id.get.underlying mustBe 1
        res1.affectedThings.get.length mustBe 2
      }
      "successfully update a treatment" in {

        val jso = addDummyTreatment()
        jso.status mustBe CREATED
        val eventId = (jso.json \ "id").as[EventId]
        eventId.underlying mustBe 2
        val oids = Seq(
          "7ae2521e-904c-432b-998c-bb09810310a9",
          "baab2f60-4f49-40fe-99c8-174b13b12d46",
          "376d41e7-c463-45e8-9bde-7a2c9844637e"
        )

        val updJson = jso.json.as[JsObject] ++ Json.obj(
          "note"           -> "Updated note",
          "eventTypeId"    -> treatmentEventTypeId, // Should not be modified by the server.
          "doneBy"         -> FakeUsers.testUserId,
          "doneDate"       -> time.dateTimeNow.plusDays(20),
          "completedBy"    -> FakeUsers.testUserId,
          "completedDate"  -> time.dateTimeNow.plusDays(20),
          "caseNumber"     -> "666",
          "affectedThings" -> oids
        )
        val updRes = putTreatmentResponse(mid, eventId, updJson)
        updRes.status mustBe OK

        val mdatetime = time.dateTimeNow.plusDays(20)
        (updRes.json \ "id").as[Int] mustBe 2
        (updRes.json \ "eventTypeId").as[Int] mustBe treatmentEventTypeId
        (updRes.json \ "note").as[String] must include("Updated")
        (updRes.json \ "updatedBy").asOpt[ActorId] mustBe Some(adminId)
        (updRes.json \ "updatedDate").asOpt[DateTime] mustApproximate Some(mdatetime)
//        (updRes.json \ "doneBy").asOpt[ActorId] mustBe Some(testUserId)
//        (updRes.json \ "doneDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "completedBy").asOpt[ActorId] mustBe Some(testUserId)
        (updRes.json \ "completedDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "caseNumber").asOpt[String] mustBe Some("666")
        (updRes.json \ "affectedThings")
          .asOpt[Seq[String]]
          .map(s => s.sorted) mustBe Some(
          oids.sorted
        )
        (updRes.json \ "registeredDate").asOpt[DateTime] mustApproximate Some(mdatetime)

      }

      "return FORBIDDEN when trying to update a treatment without permissions" in {

        val updJson = Json.obj(
          "note" -> "Updated2 note"
        )
        val updRes = putTreatmentResponse(mid, 2L, updJson, tokenRead)
        updRes.status mustBe FORBIDDEN

      }
      "return not OK when update a treatment with another eventId than" +
        "the Id in JSon " in {
        val jso = addDummyTreatment()
        jso.status mustBe CREATED

        val updJson = jso.json.as[JsObject] ++ Json.obj(
          "id"          -> 300,
          "note"        -> "Updated note",
          "eventTypeId" -> treatmentEventTypeId, // Should not be modified by the server.
          "updatedBy"   -> adminId,
          "updatedDate" -> time.dateTimeNow.plusDays(20)
        )

        val updRes = putTreatmentResponse(mid, 4L, updJson)

        assert(updRes.status !== OK)

      }
      "return the list of materials in treatment event" in {
        val res =
          wsUrl(getTreatmentMaterial)
            .withHttpHeaders(tokenRead.asHeader)
            .get()
            .futureValue
        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 73
        (res.json \ 0 \ "noTerm").as[String] mustBe "Sitronsyre"
        (res.json \ 0 \ "id").as[Int] mustBe 1
      }
      "return the list of keywords in treatment event" in {
        val res =
          wsUrl(getKeywordMaterial).withHttpHeaders(tokenRead.asHeader).get().futureValue
        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 16
        (res.json \ 0 \ "noTerm").as[String] mustBe "Støvsuget"
        (res.json \ 0 \ "id").as[Int] mustBe 1
      }
    }
  }
}
