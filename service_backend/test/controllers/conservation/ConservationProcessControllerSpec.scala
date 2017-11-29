package controllers.conservation

import models.conservation.events._
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.test.Helpers._
import repositories.conservation.dao.ConservationEventDao

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
        Some(DateTime.now),
        Some("testKommentar"),
        Some("777"),
        Some(testAffectedThings)
      )

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

  val standaloneTreatmentId               = 4L
  val compositeConservationProcessEventId = standaloneTreatmentId + 1
  val treatmentId                         = compositeConservationProcessEventId + 2 //The second child
  val treatmentIdWithActors               = treatmentId + 2 // one spesific treatment to check for later

  val edate = DateTime.now

  "Using the conservationProcess controller" when {

    "fetching conservationProcess types" should {

      "return all event types" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 7
      }
    }
    "working with conservationProcess" should {

      "add a new conservationProcess" in {

        val res = addDummyConservationProcess()
        res.status mustBe CREATED // creates ids 1 to 2
        (res.json \ "id").as[Int] mustBe 1
        (res.json \ "registeredBy").asOpt[String] mustBe Some(
          "d63ab290-2fab-42d2-9b57-2475dfbd0b3c"
        )
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

        val updJson = Json.obj(
          "id"             -> eventId,
          "note"           -> "Updated note",
          "eventTypeId"    -> conservationProcessEventTypeId, // Should not be modified by the server.
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
        (updRes.json \ "completedBy").asOpt[ActorId] mustBe Some(testUserId)
        (updRes.json \ "completedDate").asOpt[DateTime] mustApproximate Some(mdatetime)
        (updRes.json \ "caseNumber").asOpt[String] mustBe Some("666")
        (updRes.json \ "affectedThings").asOpt[Seq[String]].get.length mustBe 3
        /* (updRes.json \ "affectedThings").asOpt[Seq[String]] must include("7ae2521e-904c-432b-998c-bb09810310a9")
        (updRes.json \ "affectedThings").asOpt[Seq[String]] must include ("baab2f60-4f49-40fe-99c8-174b13b12d46")
        (updRes.json \ "affectedThings").asOpt[Seq[String]] must include ("376d41e7-c463-45e8-9bde-7a2c9844637e")*/
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
          "eventTypeId" -> conservationProcessEventTypeId // Should not be modified by the server.
        )

        val updRes = putEvent(3L, updJson)

        assert(updRes.status == BAD_REQUEST)
        (updRes.json \ "message").as[String] must include("Inconsistent")

      }

      "add standalone treatment having data in one of the 'extra' attributes" in {

        val treatmentJson = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "en annen fin treatment",
          "materials"      -> Seq(1, 2, 3),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            ),
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(10)
            )
          )
        )

        val res = postEvent(treatmentJson)

        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe standaloneTreatmentId

        val treatment = getEventObject(eventId).asInstanceOf[Treatment]
        treatment.actorsAndRoles.get.length mustBe 2
        val myActors = treatment.actorsAndRoles.get.sortBy(_.roleId)

        myActors.head.roleId mustBe 1
      }

      val oids = Seq(
        ObjectUUID.unsafeFromString("7ae2521e-904c-432b-998c-bb09810310a9"),
        ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"),
        ObjectUUID.unsafeFromString("376d41e7-c463-45e8-9bde-7a2c9844637e")
      )

      "add composite ConservationProcess (ie with children)" in {

        val treatment1 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "en fin treatment",
          "affectedThings" -> Seq("baab2f60-4f49-40fe-99c8-174b13b12d46"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(1)
            ),
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(2)
            )
          )
        )

        val treatment2 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "en annen fin treatment",
          "materials"      -> Seq(1, 2, 3),
          "affectedThings" -> Seq("376d41e7-c463-45e8-9bde-7a2c9844637e"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(3)
            ),
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(4)
            )
          )
        )

        val json = Json.obj(
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(treatment1, treatment2),
          "affectedThings" -> oids,
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            ),
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(6)
            )
          )
        )

        val res = postEvent(json)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe compositeConservationProcessEventId

        val cpr = getEventObject(eventId).asInstanceOf[ConservationProcess]
        cpr.actorsAndRoles.get.length mustBe 2
        val cprActors = cpr.actorsAndRoles.get.sortBy(_.roleId)
        cprActors.head.roleId mustBe 1

        val trm1 = cpr.events.map { m =>
          val first = m.head.id
          first.map(eventId => {
            val trm = getEventObject(eventId)
            trm.actorsAndRoles.get.length mustBe 2
            val trmActors = trm.actorsAndRoles.get.sortBy(_.roleId)
            trmActors.head.roleId mustBe 1
          })
        }
        val trm2 = cpr.events.map { m =>
          val second = m.tail.head.id
          second.map(eventId => {
            val trm = getEventObject(eventId)
            trm.actorsAndRoles.get.length mustBe 2
            val trmActors = trm.actorsAndRoles.get.sortBy(_.roleId)
            trmActors.head.roleId mustBe 2
          })
        }

      }

      "get composite ConservationProcess (ie with children)" in {
        val res = getEvent(compositeConservationProcessEventId)
        res.status mustBe OK
        val consProcess = res.json.validate[ConservationProcess].get
        consProcess.events.get.length must be >= 2
        consProcess.registeredBy must not be None
        //consProcess.affectedThings mustBe Some(oids)
        consProcess.affectedThings.get.length mustBe 3
        val firstEvent = consProcess.events.get.head
        firstEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"))
        )
      }

      "get a children of the composite ConservationProcess separately" in {
        val res = getEvent(compositeConservationProcessEventId + 1)
        res.status mustBe OK

        implicit val reads = Treatment.reads
        val treatment      = res.json.validate[Treatment].get
        treatment.partOf mustBe Some(EventId(compositeConservationProcessEventId))
        treatment.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"))
        )

      }

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
        val edate           = DateTime.now
        val treatment1 = Json.obj(
          "eventTypeId" -> treatmentEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "note"        -> "en fin treatment 3",
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          )
        )

        val updatedMaterials = Seq(2)

        val treatment2 = Json.obj(
          "id"          -> treatmentId,
          "eventTypeId" -> treatmentEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "note"        -> "Endret kommentar på treatment2",
          "materials"   -> updatedMaterials
        )

        val treatment3 = Json.obj(
          "eventTypeId" -> treatmentEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "note"        -> "ny treatment3",
          "materials"   -> updatedMaterials,
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            ),
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(6)
            ),
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(3)
            )
          )
        )

        val json = Json.obj(
          "id"          -> compositeConservationProcessEventId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "events"      -> Json.arr(treatment1, treatment2, treatment3),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          )
        )
        //time.dateTimeNow.plusDays(20)

        val updRes = putEvent(compositeConservationProcessEventId, json)

        updRes.status mustBe OK

        val treatmentAfterUpdate =
          getEventObject(treatmentIdWithActors).asInstanceOf[Treatment]

        treatmentAfterUpdate.note mustBe Some("ny treatment3")
        treatmentAfterUpdate.materials mustBe Some(updatedMaterials)
        treatmentAfterUpdate.registeredDate must not be None
        treatmentAfterUpdate.actorsAndRoles.map(a => a.tail.head.roleId mustBe 2)
        treatmentAfterUpdate.actorsAndRoles.get.length mustBe 3

        val cp = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]

        cp.events.get.length must be >= 3
        cp.actorsAndRoles.get.length mustBe 1
      }

      "Return not OK when update an subevent with wrong eventId" in {

        val updTreatment = Json.obj(
          "id"          -> 666,
          "eventTypeId" -> treatmentEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "note"        -> "Endret kommentar på treatment med feil eventid"
        )

        val jsonCp = Json.obj(
          "id"          -> compositeConservationProcessEventId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "events"      -> Json.arr(updTreatment)
        )

        val updTreat = putEvent(compositeConservationProcessEventId, jsonCp)
        updTreat.status mustBe BAD_REQUEST

      }

      "Get the list of events from an object, by it's objectUuid" in {
        val techDescrJson = Json.obj(
          "eventTypeId"    -> technicalDescriptionEventTypeId,
          "doneBy"         -> adminId,
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

      "update composite event with new updatedBy and updatedDate " in {

        val treatment1 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "note"           -> "en fin treatment på id 11",
          "affectedThings" -> Seq("baab2f60-4f49-40fe-99c8-174b13b12d46")
        )

        val updatedMaterials = Seq(2)
        val dateForChange    = time.dateTimeNow.plusDays(20)
        val treatment2 = Json.obj(
          "id"             -> treatmentId,
          "eventTypeId"    -> treatmentEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "note"           -> "Endret kommentar på treatment2",
          "materials"      -> updatedMaterials,
          "affectedThings" -> Seq("7ae2521e-904c-432b-998c-bb09810310a9")
        )
        val treatment3 = Json.obj(
          "id"          -> treatmentIdWithActors, //earlier with actors should no be without actors
          "eventTypeId" -> treatmentEventTypeId,
          "doneBy"      -> adminId,
          "completedBy" -> adminId,
          "note"        -> "ny treatment6663",
          "materials"   -> updatedMaterials
        )

        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(treatment1, treatment2, treatment3),
          "affectedThings" -> oids
        )
        //time.dateTimeNow.plusDays(20)
        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val treatmentAfterUpdate =
          getEventObject(treatmentId, token).asInstanceOf[Treatment]
        treatmentAfterUpdate.note mustBe Some("Endret kommentar på treatment2")
        treatmentAfterUpdate.materials mustBe Some(updatedMaterials)
        treatmentAfterUpdate.updatedBy mustBe Some(adminId)
        treatmentAfterUpdate.updatedDate mustApproximate Some(edate)
        treatmentAfterUpdate.registeredBy mustBe Some(adminId)
        treatmentAfterUpdate.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("7ae2521e-904c-432b-998c-bb09810310a9"))
        )

        val newSubTreatment =
          getEventObject(treatmentIdWithActors + 2).asInstanceOf[Treatment]
        newSubTreatment.note mustBe Some("en fin treatment på id 11")
        newSubTreatment.updatedBy mustBe None
        newSubTreatment.updatedDate mustBe None
        newSubTreatment.registeredDate mustApproximate Some(edate)
        newSubTreatment.registeredBy mustBe Some(adminId)

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.updatedBy mustBe Some(adminId)
        cpe.updatedDate mustApproximate Some(edate)
        cpe.registeredBy mustBe Some(adminId)
        cpe.registeredDate mustApproximate Some(edate)
        cpe.events.get.length must be >= 5
        val subEvent = cpe.events.get.head
        subEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"))
        )

        val gmlSubTreatment =
          getEventObject(treatmentIdWithActors).asInstanceOf[Treatment]
        gmlSubTreatment.note mustBe Some("ny treatment6663")
        gmlSubTreatment.actorsAndRoles.get.length mustBe 0
      }
    }
    "working with subevents " should {

      val standaloneStorageAndHandlingId = 12L
      "add standalone storageAndHandling having data in one of the 'extra' attributes" in {

        val sahJson = Json.obj(
          "eventTypeId"      -> storageAndHandlingEventTypeId,
          "doneBy"           -> adminId,
          "completedBy"      -> adminId,
          "note"             -> "en ny og fin oppbevaringOgHåndtering",
          "relativeHumidity" -> " >20% ",
          "lightAndUvLevel"  -> "mye uv",
          "temperature"      -> "30+",
          "affectedThings"   -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            ),
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(10)
            )
          )
        )
        val res = postEvent(sahJson)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe standaloneStorageAndHandlingId

        val sah = getEventObject(eventId).asInstanceOf[StorageAndHandling]
        sah.actorsAndRoles.get.length mustBe 2
        val myActors = sah.actorsAndRoles.get.sortBy(_.roleId)
        sah.relativeHumidity mustBe Some(" >20% ")
        sah.lightAndUvLevel mustBe Some("mye uv")
        sah.temperature mustBe Some("30+")

        myActors.head.roleId mustBe 1
      }
      "Post a new storageAndHandle event to our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "eventTypeId"      -> storageAndHandlingEventTypeId,
          "note"             -> "den nyeste og fineste oppbevaringOgHåndtering",
          "relativeHumidity" -> " >30% ",
          "lightAndUvLevel"  -> "mye mer uv",
          "temperature"      -> "30 minus",
          "affectedThings"   -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            ),
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(10)
            )
          )
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val newSubStorageAndHandle =
          getEventObject(standaloneStorageAndHandlingId + 1)
            .asInstanceOf[StorageAndHandling]
        newSubStorageAndHandle.note mustBe Some(
          "den nyeste og fineste oppbevaringOgHåndtering"
        )
        newSubStorageAndHandle.updatedBy mustBe None
        newSubStorageAndHandle.updatedDate mustBe None
        newSubStorageAndHandle.registeredDate mustApproximate Some(edate)
        newSubStorageAndHandle.registeredBy mustBe Some(adminId)

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === storageAndHandlingEventTypeId
        ) mustBe true

        val subEvent = cpe.events.get.head
        subEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"))
        )
      }

      "update the storageAndHandle event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"               -> (standaloneStorageAndHandlingId + 1),
          "eventTypeId"      -> storageAndHandlingEventTypeId,
          "note"             -> "endring av oppbevaringOgHåndtering",
          "relativeHumidity" -> " >30% ",
          "lightAndUvLevel"  -> "mye mer uv",
          "temperature"      -> "30 pluss",
          "affectedThings"   -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(10)
            )
          )
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newSubStorageAndHandle =
          getEventObject(standaloneStorageAndHandlingId + 1)
            .asInstanceOf[StorageAndHandling]
        newSubStorageAndHandle.note mustBe Some(
          "endring av oppbevaringOgHåndtering"
        )
        newSubStorageAndHandle.temperature mustBe Some("30 pluss")
        newSubStorageAndHandle.lightAndUvLevel mustBe Some("mye mer uv")
        newSubStorageAndHandle.actorsAndRoles.get.length mustBe 1
      }

      val hseRiskAssessmentId = standaloneStorageAndHandlingId + 1
      "Post a new hseRiskAssessment event to our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "eventTypeId"    -> hseRiskAssessmentEventTypeId,
          "note"           -> "den nyeste og fineste hmsrisiko-en",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            )
          )
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val newSubHseRiskAssessment =
          getEventObject(hseRiskAssessmentId + 1).asInstanceOf[HseRiskAssessment]
        newSubHseRiskAssessment.note mustBe Some(
          "den nyeste og fineste hmsrisiko-en"
        )
        newSubHseRiskAssessment.updatedBy mustBe None
        newSubHseRiskAssessment.updatedDate mustBe None
        newSubHseRiskAssessment.registeredDate mustApproximate Some(edate)
        newSubHseRiskAssessment.registeredBy mustBe Some(adminId)

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === hseRiskAssessmentEventTypeId
        ) mustBe true

        val subEvent = cpe.events.get.head
        subEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("baab2f60-4f49-40fe-99c8-174b13b12d46"))
        )
      }
      "update the hseRiskAssessment event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 1),
          "eventTypeId"    -> hseRiskAssessmentEventTypeId,
          "note"           -> "endring av HMS",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newSubHseRiskAss =
          getEventObject(hseRiskAssessmentId + 1).asInstanceOf[HseRiskAssessment]
        newSubHseRiskAss.note mustBe Some(
          "endring av HMS"
        )
        newSubHseRiskAss.actorsAndRoles.get.length mustBe 0
      }
      "Post a new condition assessment event to our cp compositeConservationProcessEventId" in {
        val caJson = Json.obj(
          "eventTypeId"    -> conditionAssessmentEventTypeID,
          "note"           -> "den nyeste og fineste tilstandsvurderingen",
          "conditionCode"  -> 2,
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            )
          )
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(caJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val newSubCondAssessment =
          getEventObject(hseRiskAssessmentId + 2).asInstanceOf[ConditionAssessment]
        newSubCondAssessment.note mustBe Some(
          "den nyeste og fineste tilstandsvurderingen"
        )
        newSubCondAssessment.updatedBy mustBe None
        newSubCondAssessment.updatedDate mustBe None
        newSubCondAssessment.registeredDate mustApproximate Some(edate)
        newSubCondAssessment.registeredBy mustBe Some(adminId)
        newSubCondAssessment.conditionCode mustBe Some(2)

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === conditionAssessmentEventTypeID
        ) mustBe true

      }
      "update the conditionAssessment event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 2),
          "eventTypeId"    -> conditionAssessmentEventTypeID,
          "note"           -> "endring av Tilstandsvurderingen",
          "conditionCode"  -> 0,
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newSubCondAss =
          getEventObject(hseRiskAssessmentId + 2).asInstanceOf[ConditionAssessment]
        newSubCondAss.note mustBe Some(
          "endring av Tilstandsvurderingen"
        )
        newSubCondAss.actorsAndRoles.get.length mustBe 0
        newSubCondAss.conditionCode mustBe Some(0)
      }
      "Post a new report event to our cp compositeConservationProcessEventId" in {
        val caJson = Json.obj(
          "eventTypeId"    -> reportEventTypeId,
          "note"           -> "den nyeste og fineste rapporten",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            )
          )
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(caJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

        val newSubReport =
          getEventObject(hseRiskAssessmentId + 3).asInstanceOf[Report]
        newSubReport.note mustBe Some(
          "den nyeste og fineste rapporten"
        )
        newSubReport.updatedBy mustBe None
        newSubReport.updatedDate mustBe None
        newSubReport.registeredDate mustApproximate Some(edate)
        newSubReport.registeredBy mustBe Some(adminId)

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === reportEventTypeId
        ) mustBe true

      }
      "update the report event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 3),
          "eventTypeId"    -> reportEventTypeId,
          "note"           -> "endring av rapporten",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newSubReport =
          getEventObject(16).asInstanceOf[Report]
        newSubReport.note mustBe Some(
          "endring av rapporten"
        )
        newSubReport.actorsAndRoles.get.length mustBe 0
      }

    }
  }
}
