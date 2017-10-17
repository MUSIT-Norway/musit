package controllers.conservation

import models.conservation.events.ConservationProcess
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
//test-only controllers.conservation.ConservationProcessControllerSpec

class ConservationProcessControllerSpec
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
  val typesUrl     = (mid: Int) => s"${baseUrl(mid)}/types"

  val getEventByIdUrl = (mid: Int) => (id: Long) => s"${baseEventUrl(mid)}/$id"

  val addConservationProcessUrl = baseEventUrl
  val getConservationProcessUrl = baseEventUrl
  val getConservationProcessByIdUrl = (mid: Int) =>
    (id: Long) => s"${baseEventUrl(mid)}/$id"
  val putConservationProcessByIdUrl = (mid: Int) =>
    (id: Long) => s"${baseEventUrl(mid)}/$id"

  def postEvent(json: JsObject, t: BearerToken = token) = {
    wsUrl(baseEventUrl(mid)).withHttpHeaders(t.asHeader).post(json).futureValue
  }

  def getEvent(eventId: Long, t: BearerToken = token) = {
    wsUrl(getEventByIdUrl(mid)(eventId)).withHttpHeaders(t.asHeader).get().futureValue
  }

  def addDummyConservationProcess(t: BearerToken = token) = {
    val js =
      dummyConservationProcessJSON(
        conservationProcessEventTypeId,
        Some(dateTimeNow),
        Some("testKommentar"),
        Some("777"),
        Some(testAffectedThings)
      )
    println("json: " + js)
    wsUrl(addConservationProcessUrl(mid)).withHttpHeaders(t.asHeader).post(js).futureValue
  }

  implicit val minReads = ConservationProcess.reads

  def getConservationProcess(
      mid: MuseumId,
      eventId: EventId,
      t: BearerToken = token
  ): ConservationProcess = {
    val cp = wsUrl(getConservationProcessByIdUrl(mid)(eventId))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
    println("skal validere json i getConservationPRocess: " + cp.json)
    cp.json.validate[ConservationProcess].get
  }

  def putConservationProcessResponse(
      mid: MuseumId,
      eventId: EventId,
      json: JsObject,
      t: BearerToken = token
  ): WSResponse = {
    wsUrl(putConservationProcessByIdUrl(mid)(eventId))
      .withHttpHeaders(t.asHeader)
      .put(json)
      .futureValue
  }

  def putConservationProcess(
      mid: MuseumId,
      eventId: EventId,
      json: JsObject,
      t: BearerToken = token
  ): ConservationProcess = {
    val cp = putConservationProcessResponse(mid, eventId, json, t)
    cp.json.validate[ConservationProcess].get
  }

  "Using the conservationProcess controller" when {

    "fetching conservationProcess types" should {

      "return all event types" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 3
      }
    }
    "working with conservationProcess" should {

      "add a new conservationProcess" in {

        val res = addDummyConservationProcess()
        println("res.body: " + res.body)

        res.status mustBe CREATED // creates ids 1 to 2
        (res.json \ "id").as[Int] mustBe 1
      }
      "get a conservationProcess by it's ID" in {
        val eventId = 1L
        val res1    = getConservationProcess(mid, eventId)

        res1.eventTypeId.underlying mustBe 1
        res1.id.get.underlying mustBe 1
      }
      "successfully update a conservation process" in {

        val jso = addDummyConservationProcess()
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
          "eventTypeId"    -> conservationProcessEventTypeId, // Should not be modified by the server.
          "doneBy"         -> FakeUsers.testUserId,
          "doneDate"       -> time.dateTimeNow.plusDays(20),
          "completedBy"    -> FakeUsers.testUserId,
          "completedDate"  -> time.dateTimeNow.plusDays(20),
          "caseNumber"     -> "666",
          "affectedThings" -> oids
        )
        val updRes = putConservationProcessResponse(mid, eventId, updJson)
        updRes.status mustBe OK

        val mdatetime = time.dateTimeNow.plusDays(20)
        (updRes.json \ "id").as[Int] mustBe 2
        (updRes.json \ "eventTypeId").as[Int] mustBe 1
        (updRes.json \ "note").as[String] must include("Updated")
        (updRes.json \ "updatedBy").asOpt[ActorId] mustBe Some(adminId)
        (updRes.json \ "updatedDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "doneBy").asOpt[ActorId] mustBe Some(testUserId)
        (updRes.json \ "doneDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "completedBy").asOpt[ActorId] mustBe Some(testUserId)
        (updRes.json \ "completedDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "caseNumber").asOpt[String] mustBe Some("666")
        (updRes.json \ "affectedThings").asOpt[Seq[String]] mustBe Some(oids)
        (updRes.json \ "registeredDate").asOpt[DateTime] mustApproximate Some(mdatetime)

      }

      "return FORBIDDEN when trying to update a conservation process without permissions" in {

        val updJson = Json.obj(
          "note" -> "Updated2 note"
        )
        val updRes = putConservationProcessResponse(mid, 2L, updJson, tokenRead)
        updRes.status mustBe FORBIDDEN

      }
      "return not OK when update a conservation process with another eventId than" +
        "the Id in JSon " in {
        val jso = addDummyConservationProcess()
        jso.status mustBe CREATED

        val updJson = jso.json.as[JsObject] ++ Json.obj(
          "id"          -> 300,
          "note"        -> "Updated note",
          "eventTypeId" -> conservationProcessEventTypeId, // Should not be modified by the server.
          "updatedBy"   -> adminId,
          "updatedDate" -> time.dateTimeNow.plusDays(20)
        )

        val updRes = putConservationProcessResponse(mid, 4L, updJson)

        assert(updRes.status !== OK)

      }
      val compositeConservationProcessEventId = 4L

      "add composite ConservationProcess (ie with children)" in {

        val treatment1 = Json.obj(
          "eventTypeId"  -> treatmentEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "note"         -> "en fin treatment"
        )

        val treatment2 = Json.obj(
          "eventTypeId"  -> treatmentEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "note"         -> "en annen fin treatment"
        )

        val json = Json.obj(
          "eventTypeId"  -> conservationProcessEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "events"       -> Json.arr(treatment1, treatment2)
        )

        val res = postEvent(json)
        println("complex add: " + res)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe compositeConservationProcessEventId
      }

      "get composite ConservationProcess (ie with children)" in {
        val res = getEvent(compositeConservationProcessEventId)
        res.status mustBe OK

        println("res.body: " + res.body)

        val consProcess = res.json.validate[ConservationProcess].get

        consProcess.events.get.length must be >= 2
      }
    }
  }
}
