package controllers.conservation

import models.conservation.events.{
  ConservationModuleEvent,
  ConservationProcess,
  Treatment
}
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

  val eventByIdUrl = (mid: Int) => (id: Long) => s"${baseEventUrl(mid)}/$id"

  val eventsByObjectUuid = (mid: Int) =>
    (id: String) => s"/$mid/conservation/events/object/$id"

  def postEvent(json: JsObject, t: BearerToken = token) = {
    wsUrl(baseEventUrl(mid)).withHttpHeaders(t.asHeader).post(json).futureValue
  }

  def getEvent(eventId: Long, t: BearerToken = token) = {
    wsUrl(eventByIdUrl(mid)(eventId)).withHttpHeaders(t.asHeader).get().futureValue
  }

  def putEvent(eventId: Long, json: JsObject, t: BearerToken = token) = {
    wsUrl(eventByIdUrl(mid)(eventId)).withHttpHeaders(t.asHeader).put(json).futureValue
  }

  def getEventForObject(oid: String, t: BearerToken = token) = {
    wsUrl(eventsByObjectUuid(mid)(oid)).withHttpHeaders(t.asHeader).get().futureValue
  }

  implicit val minReads = ConservationModuleEvent.reads
  implicit val cpReads  = ConservationProcess.reads

  def getEventObject(
      eventId: Long,
      t: BearerToken = token
  ) = {
    val res = getEvent(eventId, t)
    res.json.validate[ConservationModuleEvent].get
  }

  def addDummyConservationProcess(t: BearerToken = token) = {
    val js =
      dummyEventJSON(
        conservationProcessEventTypeId,
        Some(dateTimeNow),
        Some("testKommentar"),
        Some("777"),
        Some(testAffectedThings)
      )
    //println("json: " + js)
    postEvent(js)
  }

  def getConservationProcess(
      mid: MuseumId,
      eventId: EventId,
      t: BearerToken = token
  ): ConservationProcess = {
    val cp = getEvent(eventId, t)

    cp.json.validate[ConservationProcess].get
  }

  def putConservationProcess(
      eventId: EventId,
      json: JsObject,
      t: BearerToken = token
  ): ConservationProcess = {
    val cp = putEvent(eventId, json, t)
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
        val updRes = putEvent(eventId, updJson)
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
        val updRes = putEvent(2L, updJson, tokenRead)
        updRes.status mustBe FORBIDDEN

      }
      "return not OK when update a conservation process with another eventId than" +
        "the Id in JSon " in {
        val jso = addDummyConservationProcess()
        jso.status mustBe CREATED

        val updJson = jso.json.as[JsObject] ++ Json.obj(
          "id"          -> 200,
          "note"        -> "Updated note",
          "eventTypeId" -> conservationProcessEventTypeId, // Should not be modified by the server.
          "updatedBy"   -> adminId,
          "updatedDate" -> time.dateTimeNow.plusDays(20)
        )

        val updRes = putEvent(3L, updJson)
        println(updRes.body)
        assert(updRes.status == BAD_REQUEST)
        (updRes.json \ "message").as[String] must include("Inconsistent")

      }

      val standaloneTreatmentId = 4L

      "add standalone treatment having data in one of the 'extra' attributes" in {

        val treatmentJson = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "registeredBy"   -> adminId,
          "updatedBy"      -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "en annen fin treatment",
          "materials"      -> Seq(1, 2, 3),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val res = postEvent(treatmentJson)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe standaloneTreatmentId

      }

      val compositeConservationProcessEventId = standaloneTreatmentId + 1

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
          "note"         -> "en annen fin treatment",
          "materials"    -> Seq(1, 2, 3)
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
        //        println("complex add: " + res)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe compositeConservationProcessEventId
      }

      "get composite ConservationProcess (ie with children)" in {
        val res = getEvent(compositeConservationProcessEventId)
        res.status mustBe OK

        val consProcess = res.json.validate[ConservationProcess].get

        consProcess.events.get.length must be >= 2
      }

      "get a children of the composite ConservationProcess separately" in {
        val res = getEvent(compositeConservationProcessEventId + 1)
        res.status mustBe OK

        implicit val reads = Treatment.reads
        val treatment      = res.json.validate[Treatment].get

        treatment.partOf mustBe Some(EventId(compositeConservationProcessEventId))
      }

      val treatmentId = compositeConservationProcessEventId + 2 //The second child

      "update a child of the composite ConservationProcess separately and " +
        "get it back in the conservationProcess" in {
        // This test is important, to check that we don't get the child
        // event back from the json blob in the parent (perhaps not updated),
        // we want it back from the event-table, where it has been updated

        val res = getEvent(treatmentId)
        res.status mustBe OK

        implicit val reads = Treatment.reads
        val treatment      = res.json.validate[Treatment].get

        treatment.partOf mustBe Some(EventId(compositeConservationProcessEventId))

        val newMaterials = Seq(10, 20, 30, 5521)

        val updJson = Json.toJson(treatment).asInstanceOf[JsObject] ++ Json.obj(
          "materials" -> newMaterials
        )
        val updRes = putEvent(treatmentId, updJson)
        updRes.status mustBe OK

        val treatmentAfterUpdate = getEventObject(treatmentId).asInstanceOf[Treatment]
        treatmentAfterUpdate.materials mustBe Some(newMaterials)

        val cp = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        val treatAfter = cp.events.get(1).asInstanceOf[Treatment]
        treatAfter.materials mustBe Some(newMaterials)
      }

      "update the composite ConservationProcess" in {
        implicit val writes = ConservationProcess.writes

        val treatment1 = Json.obj(
          "eventTypeId"  -> treatmentEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "note"         -> "en fin treatment 3"
        )

        val updatedMaterials = Seq(2)

        val treatment2 = Json.obj(
          "id"           -> treatmentId,
          "eventTypeId"  -> treatmentEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "note"         -> "Endret kommentar på treatment2",
          "materials"    -> updatedMaterials
        )

        val json = Json.obj(
          "id"           -> compositeConservationProcessEventId,
          "eventTypeId"  -> conservationProcessEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "events"       -> Json.arr(treatment1, treatment2)
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val treatmentAfterUpdate = getEventObject(treatmentId).asInstanceOf[Treatment]
        treatmentAfterUpdate.materials mustBe Some(updatedMaterials)

        val cp = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]

        cp.events.get.length must be >= 3
      }
      "Return not OK when update an subevent with wrong eventId" in {

        val updTreatment = Json.obj(
          "id"           -> 666,
          "eventTypeId"  -> treatmentEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "note"         -> "Endret kommentar på treatment med feil eventid"
        )

        val jsonCp = Json.obj(
          "id"           -> compositeConservationProcessEventId,
          "eventTypeId"  -> conservationProcessEventTypeId,
          "doneBy"       -> adminId,
          "registeredBy" -> adminId,
          "updatedBy"    -> adminId,
          "completedBy"  -> adminId,
          "events"       -> Json.arr(updTreatment)
        )

        val updTreat = putEvent(compositeConservationProcessEventId, jsonCp)
        updTreat.status mustBe BAD_REQUEST
      }

      "Get the list of events from an object, by it's objectUuid" in {
        val techDescrJson = Json.obj(
          "eventTypeId"    -> technicalDescriptionEventTypeId,
          "doneBy"         -> adminId,
          "registeredBy"   -> adminId,
          "updatedBy"      -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "en annen fin techDesc",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val res = postEvent(techDescrJson)
        res.status mustBe CREATED

        val oid    = "42b6a92e-de59-4fde-9c46-5c8794be0b34"
        val events = getEventForObject(oid)
        events.status mustBe OK
        //return both subevents, an earlier treatment and above techDescription
        events.json.as[JsArray].value.size mustBe 2

      }

      "Return No_content(204) when objectUuid has no events" in {
        val oid    = "376d41e7-c463-45e8-9bde-7a2c9844637e"
        val events = getEventForObject(oid)
        events.status mustBe NO_CONTENT
      }
    }
  }
}
