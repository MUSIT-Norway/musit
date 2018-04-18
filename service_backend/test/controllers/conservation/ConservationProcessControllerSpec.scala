package controllers.conservation

import models.conservation.events._
import models.conservation.{
  ConservationProcessKeyData,
  MaterialArchaeology,
  MaterialEthnography,
  MaterialNumismatic
}
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import org.scalatest.Inspectors.forAll
import play.api.libs.json._
import play.api.test.Helpers._

//Hint, to run only this test, type:
//test-only controllers.conservation.ConservationProcessControllerSpec

class ConservationProcessControllerSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers
    with ConservationJsonGenerators
    with ConservationJsonValidators {
  val mid          = MuseumId(99)
  val token        = BearerToken(FakeUsers.testAdminToken)
  val tokenRead    = BearerToken(FakeUsers.testReadToken)
  val tokenTest    = BearerToken(FakeUsers.testUserToken)
  val tokenNhmRead = BearerToken(FakeUsers.nhmReadToken)

  /*val (nhmReadId, nhmReadToken) =
    ("ddb4dc62-8c14-4bac-aafd-0401f619b0ac", "54aa85c8-6212-4381-8d22-1d342cd8a26e")*/

  val baseUrl      = (mid: Int) => s"/$mid/conservation"
  val baseEventUrl = (mid: Int) => s"/$mid/conservation/events"
  val typesUrl     = (mid: Int) => s"${baseUrl(mid)}/types"

  val eventByIdUrl = (mid: Int) => (id: Long) => s"${baseEventUrl(mid)}/$id"

  val eventsByObjectUuid = (mid: Int) =>
    (id: String) => s"/$mid/conservation/events/object/$id"

  val cpsKeyDataByObjectUuid = (mid: Int) =>
    (id: String) => s"/$mid/conservation/conservations/object/$id"

  val currentMaterialdataForObjectUuid = (mid: Int) =>
    (id: String) => s"/$mid/conservation/object/$id/materials"

  val currentMeasurementdataForObjectUuid = (mid: Int) =>
    (id: String) => s"/$mid/conservation/object/$id/measurements"

  val deleteSubEventsUrl = (mid: Int, eventIds: String) =>
    baseEventUrl(mid) + s"?eventIds=$eventIds"

  /* val materialListUrl = (mid: Int, collectionId: String) =>
    s"/$mid/conservation/materials?collectionId=$collectionId"*/

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

  def getCurrentMaterialDataForObject(oid: String, t: BearerToken = token) = {
    wsUrl(currentMaterialdataForObjectUuid(mid)(oid))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
  }

  def getCurrentMeasurementDataForObject(oid: String, t: BearerToken = token) = {
    wsUrl(currentMeasurementdataForObjectUuid(mid)(oid))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
  }

  def deleteEvents(eventIds: String, t: BearerToken = token) = {
    wsUrl(deleteSubEventsUrl(mid, eventIds))
      .withHttpHeaders(t.asHeader)
      .delete()
      .futureValue
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

  def MaybeGetEventObject(
      eventId: Long,
      t: BearerToken = token
  ) = {
    val res = getEvent(eventId, t)
    if (res.status == OK) Some(res.json.validate[ConservationModuleEvent].get)
    else None
  }

  def addDummyConservationProcess(t: BearerToken = token) = {
    val js =
      dummyEventJSON(
        conservationProcessEventTypeId,
        Some("testKommentar"),
        Some("777"),
        Some(testAffectedThings),
        true
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

  def getCpsKeyDataForObject(oid: String, t: BearerToken = token) = {
    wsUrl(cpsKeyDataByObjectUuid(mid)(oid)).withHttpHeaders(t.asHeader).get().futureValue
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
        res.json.as[JsArray].value.size mustBe 10
      }
    }

    /*
    "should return NOT_FOUND on an not-existing id" in {
      val res = getEvent(999, token)
      println(s"res: $res")
      res.status mustBe NOT_FOUND
    }
     */
    "should be able to get existing event without isUpdated in json" in {
      //First we tried some special eventId like 666, but then the next eventId became 667, which ruined most of our tests,
      //so then we instead used -1 as the test-event inserted in the database
      //val cp = getEventObject(-1).asInstanceOf[ConservationProcess]
      val res = getEvent(-1, token)
      res.status mustBe OK
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
          "caseNumber"     -> "666",
          "isUpdated"      -> true,
          "affectedThings" -> oids
        )

        val updRes = putEvent(eventId, updJson)

        updRes.status mustBe OK

        val mdatetime = time.dateTimeNow.plusDays(20)
        (updRes.json \ "id").as[Int] mustBe 2
        (updRes.json \ "eventTypeId").as[Int] mustBe 1
        (updRes.json \ "note").as[String] must include("Updated")
        (updRes.json \ "caseNumber").asOpt[String] mustBe Some("666")
        (updRes.json \ "affectedThings").asOpt[Seq[String]].get.length mustBe 3
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
          "isUpdated"   -> true
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
          ),
          "isUpdated" -> true
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
          ),
          "isUpdated" -> true
        )

        val treatment2 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
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
          ),
          "isUpdated" -> true
        )

        val json = Json.obj(
          "eventTypeId"    -> conservationProcessEventTypeId,
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
          ),
          "isUpdated" -> true
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
          "note"        -> "en fin treatment 3",
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          ),
          "isUpdated" -> true
        )

        val updatedMaterials = Seq(2)

        val treatment2 = Json.obj(
          "id"          -> treatmentId,
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "Endret kommentar på treatment2",
          "materials"   -> updatedMaterials,
          "isUpdated"   -> true
        )

        val treatment3 = Json.obj(
          "eventTypeId" -> treatmentEventTypeId,
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
          ),
          "isUpdated" -> true
        )

        val json = Json.obj(
          "id"          -> compositeConservationProcessEventId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "events"      -> Json.arr(treatment1, treatment2, treatment3),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          ),
          "isUpdated" -> true
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
          "note"        -> "Endret kommentar på treatment med feil eventid",
          "isUpdated"   -> true
        )

        val jsonCp = Json.obj(
          "id"          -> compositeConservationProcessEventId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "events"      -> Json.arr(updTreatment),
          "isUpdated"   -> true
        )

        val updTreat = putEvent(compositeConservationProcessEventId, jsonCp)
        updTreat.status mustBe BAD_REQUEST

      }

      "Get the list of events from an object, by it's objectUuid" in {
        val techDescrJson = Json.obj(
          "eventTypeId"    -> technicalDescriptionEventTypeId,
          "note"           -> "en annen fin techDesc",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          "note"           -> "en fin treatment på id 11",
          "affectedThings" -> Seq("baab2f60-4f49-40fe-99c8-174b13b12d46"),
          "isUpdated"      -> true
        )

        val updatedMaterials = Seq(2)
        val dateForChange    = time.dateTimeNow.plusDays(20)
        val treatment3 = Json.obj(
          "id"          -> treatmentIdWithActors, //earlier with actors should no be without actors
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "ny treatment6663",
          "materials"   -> updatedMaterials,
          "isUpdated"   -> true
        )

        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(treatment1, treatment3),
          "affectedThings" -> oids,
          "isUpdated"      -> true
        )
        //time.dateTimeNow.plusDays(20)
        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK

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
    val standaloneStorageAndHandlingId = 12L
    val hseRiskAssessmentId            = standaloneStorageAndHandlingId + 1
    "working with subevents " should {
      "add standalone storageAndHandling having data in one of the 'extra' attributes" in {

        val sahJson = Json.obj(
          "eventTypeId"      -> storageAndHandlingEventTypeId,
          "note"             -> "en ny og fin oppbevaringOgHåndtering",
          "relativeHumidity" -> " >20% ",
          "lightLevel"       -> "mye lys",
          "uvLevel"          -> "mye uv",
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
          ),
          "isUpdated" -> true
        )
        val res = postEvent(sahJson)
        res.status mustBe CREATED
        val eventId = (res.json \ "id").as[EventId]
        eventId.underlying mustBe standaloneStorageAndHandlingId

        val sah = getEventObject(eventId).asInstanceOf[StorageAndHandling]
        sah.actorsAndRoles.get.length mustBe 2
        val myActors = sah.actorsAndRoles.get.sortBy(_.roleId)
        sah.relativeHumidity mustBe Some(" >20% ")
        sah.lightLevel mustBe Some("mye lys")
        sah.uvLevel mustBe Some("mye uv")
        sah.temperature mustBe Some("30+")

        myActors.head.roleId mustBe 1
      }
      "Post a new storageAndHandle event to our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "eventTypeId"      -> storageAndHandlingEventTypeId,
          "note"             -> "den nyeste og fineste oppbevaringOgHåndtering",
          "relativeHumidity" -> " >30% ",
          "lightLeven"       -> "mye mere lys",
          "uvLevel"          -> "mye mer uv",
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
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          "lightLevel"       -> "mye mye mer lys",
          "uvLevel"          -> "mye mer uv",
          "temperature"      -> "30 pluss",
          "affectedThings"   -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> testUserId,
              "date"    -> time.dateTimeNow.plusDays(10)
            )
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
        newSubStorageAndHandle.lightLevel mustBe Some("mye mye mer lys")
        newSubStorageAndHandle.uvLevel mustBe Some("mye mer uv")
        newSubStorageAndHandle.actorsAndRoles.get.length mustBe 1
      }

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
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(caJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "doneBy"         -> adminId,
          "completedBy"    -> adminId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
          ),
          "archiveReference" -> "2017/33",
          "isUpdated"        -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(caJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
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
        newSubReport.archiveReference mustBe Some("2017/33")

        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === reportEventTypeId
        ) mustBe true

      }
      "update the report event in our cp compositeConservationProcessEventId" in {
        val fileId1 = FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c")
        val fileId2 = FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b4c")

        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 3),
          "eventTypeId"    -> reportEventTypeId,
          "note"           -> "endring av rapporten",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "documents" -> Seq(
            fileId1,
            fileId2
            //            "d63ab290-2fab-42d2-9b57-2475dfbd0b3c",
            //            "d63ab290-2fab-42d2-9b57-2475dfbd0b4c"
          ),
          "archiveReference" -> "2017/66",
          "isUpdated"        -> true
        )

        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newSubReport =
          getEventObject(16).asInstanceOf[Report]
        newSubReport.note mustBe Some(
          "endring av rapporten"
        )
        newSubReport.actorsAndRoles.isDefined mustBe true
        newSubReport.actorsAndRoles.get.length mustBe 0
        newSubReport.documents.isDefined mustBe true
        newSubReport.documents.get.length mustBe 2
        newSubReport.archiveReference mustBe Some("2017/66")

        newSubReport.documents.get.contains(fileId1) mustBe true
        newSubReport.documents.get.contains(fileId2) mustBe true
        newSubReport.documents.get.head mustBe FileId.unsafeFromString(
          "d63ab290-2fab-42d2-9b57-2475dfbd0b3c"
        )
      }
      "delete two subevents " in {
        val eventid = hseRiskAssessmentId + 2
        val sub1    = getEventObject(eventid).asInstanceOf[ConservationEvent]
        val sub2    = getEventObject(16).asInstanceOf[ConservationEvent]

        val delEvents = "16," + eventid.toString
        val oldCp = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]

        oldCp.updatedDate must not be None
        oldCp.updatedDate mustApproximate Some(dateTimeNow)

        Thread.sleep(1002)
        val de = deleteEvents(delEvents)
        de.status mustBe OK

        val sub1AfterDelete = MaybeGetEventObject(eventid)
        sub1AfterDelete.isDefined mustBe false

        val sub2AfterDelete = MaybeGetEventObject(16)
        sub2AfterDelete.isDefined mustBe false

        val cp = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]

        cp.updatedDate must not be None
        cp.updatedDate must not be oldCp.updatedDate
        cp.updatedDate mustApproximate Some(dateTimeNow)

      }
      "delete two subevents, one does not exists " in {
        val eventid   = 999
        val delEvents = "12," + eventid.toString
        val de        = deleteEvents(delEvents)
        de.status mustBe BAD_REQUEST
      }
      "delete two subevents but the delimitor is ; and not , " in {
        val eventid   = hseRiskAssessmentId
        val delEvents = s"10;$eventid"
        val de        = deleteEvents(delEvents)
        de.status mustBe BAD_REQUEST
      }
      "delete three subevents but only one exists " in {
        val eventid   = hseRiskAssessmentId
        val delEvents = s"$eventid,9999,9998"
        val de        = deleteEvents(delEvents)
        de.status mustBe BAD_REQUEST
      }
      "return Bad_Request when updating an report event that is already removed" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 3),
          "eventTypeId"    -> reportEventTypeId,
          "note"           -> "endring av hms med feil spesAttributter",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe BAD_REQUEST
      }
    }
    "working with materialdDetermination events " should {
      "Post a new materialDetermination event to our cp compositeConservationProcessEventId" in {
        val mdJson = Json.obj(
          "eventTypeId"    -> materialDeterminationEventTypeId,
          "note"           -> "den nyeste og fineste materialbestemmelsen",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            )
          ),
          "materialInfo" -> Seq(
            Json.obj(
              "materialId"    -> 1,
              "materialExtra" -> "veldig spes tre",
              "sorting"       -> 1
            )
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)

        updRes.status mustBe OK

        val newSubMaterialDet =
          getEventObject(hseRiskAssessmentId + 4).asInstanceOf[MaterialDetermination]
        newSubMaterialDet.note mustBe Some(
          "den nyeste og fineste materialbestemmelsen"
        )

        newSubMaterialDet.materialInfo.map(
          sms => sms.head.materialId mustBe 1
        )
        newSubMaterialDet.materialInfo.get.head.materialExtra mustBe Some(
          "veldig spes tre"
        )
        newSubMaterialDet.materialInfo.get.head.sorting mustBe Some(1)
        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === materialDeterminationEventTypeId
        ) mustBe true

      }
      "update the materialDetermination event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 4),
          "eventTypeId"    -> materialDeterminationEventTypeId,
          "note"           -> "endring av materialbestemmelsen",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "materialInfo" -> Seq(
            Json.obj(
              "materialId"    -> 2,
              "materialExtra" -> "Mye mer spes jern",
              "sorting"       -> 2
            ),
            Json.obj(
              "materialId"    -> 3,
              "materialExtra" -> "Mest spes sølv",
              "sorting"       -> 3
            )
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newMdEvent =
          getEventObject(hseRiskAssessmentId + 4).asInstanceOf[MaterialDetermination]
        newMdEvent.note mustBe Some(
          "endring av materialbestemmelsen"
        )
        newMdEvent.materialInfo.map(sms => sms.length mustBe 2)
        newMdEvent.materialInfo.get.head.materialId mustBe 2
        newMdEvent.materialInfo.get.head.materialExtra mustBe Some(
          "Mye mer spes jern"
        )
        newMdEvent.materialInfo.get.head.sorting mustBe Some(2)
        newMdEvent.materialInfo.get.tail.head.materialId mustBe 3
        newMdEvent.materialInfo.get.tail.head.materialExtra mustBe Some(
          "Mest spes sølv"
        )
        newMdEvent.materialInfo.get.tail.head.sorting mustBe Some(3)
      }
      "get current materialData from an object" in {

        val res = getCurrentMaterialDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        res.status mustBe OK
        val currentMaterial = res.json.validate[Seq[MaterialInfo]].get
        currentMaterial.head.materialId mustBe 2
        currentMaterial.head.materialExtra mustBe Some("Mye mer spes jern")

        //then post another materialDetermination
        val mdJson = Json.obj(
          "eventTypeId"    -> materialDeterminationEventTypeId,
          "note"           -> "den nyeste og fineste materialbestemmelsen",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "materialInfo" -> Seq(
            Json.obj(
              "materialId"    -> 1,
              "materialExtra" -> "veldig spes tre",
              "sorting"       -> 1
            )
          ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        //and then check what the new materialdata is now
        val anotherRes =
          getCurrentMaterialDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        anotherRes.status mustBe OK
        val currentMatr = anotherRes.json.validate[Seq[MaterialInfo]].get
        currentMatr.head.materialId mustBe 1
        currentMatr.head.materialExtra mustBe Some("veldig spes tre")

      }
      "return NoContent when trying to get materialData from an object that has no materials" in {
        val res = getCurrentMaterialDataForObject("baab2f60-4f49-40fe-99c8-174b13b12d46")
        res.status mustBe NO_CONTENT
      }
    }
    "working with measurementDetermination events " should {
      "Post a new measurementDetermination event to our cp compositeConservationProcessEventId" in {
        val mdJson = Json.obj(
          "eventTypeId"    -> measurementDeterminationEventTypeId,
          "note"           -> "det nyeste og fineste målet",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)

        updRes.status mustBe OK

        val newSubMeasureDet =
          getEventObject(hseRiskAssessmentId + 6).asInstanceOf[MeasurementDetermination]
        newSubMeasureDet.note mustBe Some(
          "det nyeste og fineste målet"
        )
        newSubMeasureDet.measurementData.isDefined mustBe false
        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === measurementDeterminationEventTypeId
        ) mustBe true

      }
      "update the measurementDetermination event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 6),
          "eventTypeId"    -> measurementDeterminationEventTypeId,
          "note"           -> "endring av målet",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "measurementData" ->
            Json.obj(
              "weight"             -> 2.0,
              "length"             -> 2.1,
              "width"              -> 2.2,
              "thickness"          -> 2.3,
              "height"             -> 2.4,
              "largestLength"      -> 2.5,
              "largestWidth"       -> 2.6,
              "largestThickness"   -> 2.7,
              "largestHeight"      -> 2.8,
              "diameter"           -> 2.9,
              "tverrmaal"          -> 2.10,
              "largestMeasurement" -> 2.11,
              "measurement"        -> "a lot of measurements",
              "quantity"           -> 2,
              "quantitySymbol"     -> "<",
              "fragmentQuantity"   -> 3
            ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newMsDEvent =
          getEventObject(hseRiskAssessmentId + 6).asInstanceOf[MeasurementDetermination]
        newMsDEvent.note mustBe Some(
          "endring av målet"
        )
        newMsDEvent.measurementData.isDefined mustBe true
        newMsDEvent.measurementData.get.weight mustBe Some(2.0)
        newMsDEvent.measurementData.get.length mustBe Some(2.1)
        newMsDEvent.measurementData.get.width mustBe Some(2.2)
        newMsDEvent.measurementData.get.thickness mustBe Some(2.3)
        newMsDEvent.measurementData.get.height mustBe Some(2.4)
        newMsDEvent.measurementData.get.largestLength mustBe Some(2.5)
        newMsDEvent.measurementData.get.largestWidth mustBe Some(2.6)
        newMsDEvent.measurementData.get.largestThickness mustBe Some(2.7)
        newMsDEvent.measurementData.get.largestHeight mustBe Some(2.8)
        newMsDEvent.measurementData.get.diameter mustBe Some(2.9)
        newMsDEvent.measurementData.get.tverrmaal mustBe Some(2.10)
        newMsDEvent.measurementData.get.largestMeasurement mustBe Some(2.11)
        newMsDEvent.measurementData.get.measurement mustBe Some("a lot of measurements")
        newMsDEvent.measurementData.get.quantity mustBe Some(2)
        newMsDEvent.measurementData.get.quantitySymbol mustBe Some("<")
        newMsDEvent.measurementData.get.fragmentQuantity mustBe Some(3)

      }
      "get current measurement from an object" in {

        val res =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        res.status mustBe OK
        val currentMmData = res.json.validate[MeasurementData].get
        currentMmData.weight mustBe Some(2.0)
        currentMmData.diameter mustBe Some(2.9)

        //then post another measurement
        val mdJson = Json.obj(
          "eventTypeId"    -> measurementDeterminationEventTypeId,
          "note"           -> "det nyeste og fineste målet",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "measurementData" ->
            Json.obj(
              "weight"             -> 3.0,
              "length"             -> 3.1,
              "width"              -> 3.2,
              "thickness"          -> 3.3,
              "height"             -> 3.4,
              "largestLength"      -> 3.5,
              "largestWidth"       -> 3.6,
              "largestThickness"   -> 3.7,
              "largestHeight"      -> 3.8,
              "diameter"           -> 3.9,
              "tverrmaal"          -> 3.10,
              "largestMeasurement" -> 3.11,
              "measurement"        -> "a lot of new measurements",
              "quantity"           -> 4,
              "quantitySymbol"     -> "<",
              "fragmentQuantity"   -> 5
            ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        //and then check what the new measurementData is now
        val anotherRes =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        anotherRes.status mustBe OK
        val currentMm = anotherRes.json.validate[MeasurementData].get
        currentMm.quantity mustBe Some(4)
        currentMm.largestHeight mustBe Some(3.8)

      }
      "return null when trying get current measurement from an object that has no measurements" in {
        val res =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b33")
        res.status mustBe OK
        res.body mustBe "null"
      }
      "get previous measurement from an object if the last one is deleted" in {

        val res =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        res.status mustBe OK
        val currentMmData = res.json.validate[MeasurementData].get
        currentMmData.quantity mustBe Some(4)
        currentMmData.largestHeight mustBe Some(3.8)

        //then post another measurement
        val mdJson = Json.obj(
          "eventTypeId"    -> measurementDeterminationEventTypeId,
          "note"           -> "kjempefint mål",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "measurementData" ->
            Json.obj(
              "weight"             -> 5.0,
              "length"             -> 5.1,
              "width"              -> 5.2,
              "thickness"          -> 5.3,
              "height"             -> 5.4,
              "largestLength"      -> 5.5,
              "largestWidth"       -> 5.6,
              "largestThickness"   -> 5.7,
              "largestHeight"      -> 5.8,
              "diameter"           -> 5.9,
              "tverrmaal"          -> 5.10,
              "largestMeasurement" -> 5.11,
              "measurement"        -> "super measurements",
              "quantity"           -> 5,
              "quantitySymbol"     -> "<",
              "fragmentQuantity"   -> 6
            ),
          "isUpdated" -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        //and then check what the new measurementData is now
        val anotherRes =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        anotherRes.status mustBe OK
        val currentMm = anotherRes.json.validate[MeasurementData].get
        currentMm.quantity mustBe Some(5)
        currentMm.largestHeight mustBe Some(5.8)
        val subevent =
          getEventObject(hseRiskAssessmentId + 8).asInstanceOf[MeasurementDetermination]
        subevent.measurementData.get.quantity mustBe Some(5)
        subevent.measurementData.get.largestHeight mustBe Some(5.8)

        //the delete the last measurementDetermination
        val eventid = hseRiskAssessmentId + 8
        val de      = deleteEvents(eventid.toString)
        de.status mustBe OK

        val getCurrentMd =
          getCurrentMeasurementDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        getCurrentMd.status mustBe OK
        val currentMdAgain = getCurrentMd.json.validate[MeasurementData].get
        currentMdAgain.quantity mustBe Some(4)
        currentMdAgain.largestHeight mustBe Some(3.8)
      }

    }
    "working with the subevent Note " should {
      "Post a new note event to our cp compositeConservationProcessEventId" in {
        val mdJson = Json.obj(
          "eventTypeId"    -> noteEventTypeId,
          "note"           -> "den nyeste og fineste kommentaren",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(mdJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)

        updRes.status mustBe OK

        val newSubNote =
          getEventObject(hseRiskAssessmentId + 9).asInstanceOf[Note]
        newSubNote.note mustBe Some(
          "den nyeste og fineste kommentaren"
        )
        val cpe = getEventObject(compositeConservationProcessEventId)
          .asInstanceOf[ConservationProcess]
        cpe.events.get.exists(
          m => m.eventTypeId.underlying === noteEventTypeId
        ) mustBe true

      }
      "update the note event in our cp compositeConservationProcessEventId" in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 9),
          "eventTypeId"    -> noteEventTypeId,
          "note"           -> "endring av kommentaren",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "caseNumber"     -> "2018/555",
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe OK
        val newNoteEvent =
          getEventObject(hseRiskAssessmentId + 9).asInstanceOf[Note]
        newNoteEvent.note mustBe Some("endring av kommentaren")
      }
    }
    "checking different stuff with events " should {
      "return 400 BAD-REQUEST when trying to update an event with an invalid objectUuid " in {
        val sahJson = Json.obj(
          "id"             -> (hseRiskAssessmentId + 9),
          "eventTypeId"    -> noteEventTypeId,
          "note"           -> "endring av kommentaren",
          "affectedThings" -> Seq("92b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )
        val json = Json.obj(
          "id"             -> compositeConservationProcessEventId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "events"         -> Json.arr(sahJson),
          "affectedThings" -> Seq("92b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val updRes = putEvent(compositeConservationProcessEventId, json)
        updRes.status mustBe BAD_REQUEST
      }
      "return 400 BAD-REQUEST when adding a standalone treatment with an invalid objectUUid" in {

        val treatmentJson = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "affectedThings" -> Seq("32b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 1,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(20)
            )
          ),
          "isUpdated" -> true
        )
        val res = postEvent(treatmentJson)
        res.status mustBe BAD_REQUEST
      }
      "get keydata for a conservationprocess for a spesific object" in {
        val treatment3 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "note"           -> "ny treatment",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val json = Json.obj(
          "eventTypeId"    -> conservationProcessEventTypeId,
          "caseNumber"     -> "2018/666",
          "events"         -> Json.arr(treatment3),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          ),
          "isUpdated" -> true
        )
        val newCp = postEvent(json)
        newCp.status mustBe CREATED
        val res = getCpsKeyDataForObject("42b6a92e-de59-4fde-9c46-5c8794be0b34")
        res.status mustBe OK
        val now = time.dateTimeNow
        (res.json \ 0 \ "eventId").as[Long] mustBe 5L
        (res.json \ 1 \ "eventId").as[Long] mustBe 23L
        (res.json \ 1 \ "caseNumber").as[String] mustBe "2018/666"
        (res.json \ 0 \ "registeredDate").as[DateTime] mustApproximate (now)
        (res.json \ 1 \ "registeredBy").as[ActorId] mustBe adminId
        (res.json \ 0 \ "registeredDate").as[DateTime] mustApproximate (now)
        (res.json \ 1 \ "registeredBy").as[ActorId] mustBe adminId
        val keyData    = (res.json \ 0 \ "noKeyData").as[JsArray].value.seq
        val keyData1   = (res.json \ 1 \ "noKeyData").as[JsArray].value.seq
        val enKeyData  = (res.json \ 0 \ "enKeyData").as[JsArray].value.seq
        val enKeyData1 = (res.json \ 1 \ "enKeyData").as[JsArray].value.seq
        keyData.length mustBe 6
        keyData1.length mustBe 1
        keyData.exists(p => p.toString() === "\"kommentar\"") mustBe true
        keyData1.exists(p => p.toString() === "\"behandling\"") mustBe true
        enKeyData.exists(p => p.toString() === "\"note\"") mustBe true
        enKeyData1.exists(p => p.toString() === "\"treatment\"") mustBe true
      }
      "get 204 No-content when trying to get keydata for a conservationprocess for an invalid ObjectUuid" in {
        val res = getCpsKeyDataForObject("32b6a92e-de59-4fde-9c46-5c8794be0b34")
        res.status mustBe NO_CONTENT
      }
    }

    "check ActorDates when updating Cp and subEvents " should {
      val cpId = 25L
      "return OK when updating both Cp and subEvents " in {
        val res = addDummyConservationProcess()
        res.status mustBe CREATED // creates id 25 to 26
        (res.json \ "id").as[Int] mustBe cpId
        val treatment1 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "note"           -> "ny treatmentsssss",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val json = Json.obj(
          "id"             -> cpId,
          "eventTypeId"    -> conservationProcessEventTypeId,
          "caseNumber"     -> "2018/66",
          "events"         -> Json.arr(treatment1),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          ),
          "isUpdated" -> true
        )
        val newCp = putEvent(cpId, json)
        newCp.status mustBe OK

      }
      "return BAD_REQUEST when subEvent has no isUpdated-attribute " in {
        //nothing will be updated if one of the events has no IsUpdated attribute
        val treatment1 = Json.obj(
          "id"          -> (cpId + 1),
          "eventTypeId" -> treatmentEventTypeId
        )

        val json = Json.obj(
          "id"          -> cpId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "caseNumber"  -> "2018/777",
          "events"      -> Json.arr(treatment1)
//          "isUpdated"   -> true
        )
        val updRes = putEvent(cpId, json)
        updRes.status mustBe BAD_REQUEST
        val newCp =
          getEventObject(cpId).asInstanceOf[ConservationProcess]
        newCp.caseNumber mustBe Some("2018/66") //caseNumber from previous update
      }
      "not update subEvent when isUpdated is false, even if the subEvent has changes in attributes, but" +
        "the new subEvent will be inserted " in {
        val treatment1 = Json.obj(
          "id"          -> (cpId + 1),
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "ny merknad som ikke skal lagres i basen siden isUpdated er false",
          "isUpdated"   -> false
        )
        val treatment2 = Json.obj(
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "ny subEvent som skal inn",
          "isUpdated"   -> true
        )

        val json = Json.obj(
          "id"          -> cpId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "events"      -> Json.arr(treatment1, treatment2),
          "note"        -> "ny merknad for oppdatering",
          "isUpdated"   -> true
        )
        val updRes = putEvent(cpId, json)
        updRes.status mustBe OK
        val newCp =
          getEventObject(cpId).asInstanceOf[ConservationProcess]
        newCp.note mustBe Some("ny merknad for oppdatering")
        val subEvent = getEventObject(cpId + 1).asInstanceOf[Treatment]
        subEvent.note must not be Some(
          "ny merknad som ikke skal lagres i basen siden isUpdated er false"
        )
        subEvent.note mustBe Some(
          "ny treatmentsssss" // previous change in treatments.note
        )

        val newSubEvent = getEventObject(cpId + 2).asInstanceOf[Treatment]
        newSubEvent.note mustBe Some("ny subEvent som skal inn")
        //newSubEvent.registeredBy mustBe Some("d63ab290-2fab-42d2-9b57-2475dfbd0b3c")
      }
      "check for updatedDate is not removed from CP when a subEvents is updated" in {
        val treatment1 = Json.obj(
          "id"             -> (cpId + 1),
          "eventTypeId"    -> treatmentEventTypeId,
          "note"           -> "ny treatmentsssss",
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "isUpdated"      -> true
        )

        val json = Json.obj(
          "id"             -> (cpId),
          "eventTypeId"    -> conservationProcessEventTypeId,
          "caseNumber"     -> "2018/66",
          "events"         -> Json.arr(treatment1),
          "affectedThings" -> Seq("42b6a92e-de59-4fde-9c46-5c8794be0b34"),
          "actorsAndRoles" -> Seq(
            Json.obj(
              "roleId"  -> 2,
              "actorId" -> adminId,
              "date"    -> time.dateTimeNow.plusDays(5)
            )
          ),
          "isUpdated" -> false
        )
        val Cp = putEvent(cpId, json)
        Cp.status mustBe OK
        val now = time.dateTimeNow
        val newCp =
          getEventObject(cpId).asInstanceOf[ConservationProcess]
        newCp.updatedDate.isDefined mustBe true
        newCp.updatedDate mustApproximate Some(now)
        newCp.updatedBy mustBe Some(adminId)
      }
      "check that the amount of subEvents to be updated is ok, and compare with " +
        "and do not update CP when isUpdated is false. But updatedDate and actor" +
        "must be changed in CP when one of the subEvents is updated. " in {
        val treatment1 = Json.obj(
          "id"          -> (cpId + 1),
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "nyeste merknaden",
          "isUpdated"   -> true
        )
        val treatment2 = Json.obj(
          "eventTypeId" -> treatmentEventTypeId,
          "note"        -> "ny subEvent som ikke skal insertes",
          "isUpdated"   -> false
        )

        val json = Json.obj(
          "id"          -> cpId,
          "eventTypeId" -> conservationProcessEventTypeId,
          "events"      -> Json.arr(treatment1, treatment2),
          "caseNumber"  -> "2018/55555",
          "isUpdated"   -> false
        )
        val cp =
          getEventObject(cpId).asInstanceOf[ConservationProcess]
        cp.events.map(m => m.length mustBe 2)
        val oldDate = cp.updatedDate
        cp.caseNumber mustBe None // old value
        Thread.sleep(1002)
        val newCp = putEvent(cpId, json)
        newCp.status mustBe OK
        val updatedCp = getEventObject(cpId).asInstanceOf[ConservationProcess]
        updatedCp.caseNumber mustBe None //not updated
        updatedCp.events.map(events => events.length mustBe 2)
        val subEvent1 = getEventObject(cpId + 1).asInstanceOf[Treatment]
        val subEvent2 = getEventObject(cpId + 2).asInstanceOf[Treatment]
        subEvent1.note mustBe Some("nyeste merknaden")
        subEvent2.note must not be Some("ny subEvent som ikke skal insertes") // not updated
        updatedCp.updatedDate.isDefined mustBe true
        updatedCp.updatedDate must not be oldDate

      }
    }
  }
}
