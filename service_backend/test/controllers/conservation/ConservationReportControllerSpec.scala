package controllers.conservation

import models.conservation.events._
import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.http.Status
import play.api.libs.json._
import play.api.test.Helpers._

//Hint, to run only this test, type:
//test-only controllers.conservation.ConservationReportControllerSpec

class ConservationReportControllerSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers
    with ConservationJsonGenerators
    with ConservationJsonValidators {
  val mid          = MuseumId(99)
  var cid          = "2e4f2455-1b3b-4a04-80a1-ba92715ff613"
  val token        = BearerToken(FakeUsers.testAdminToken)
  val tokenGodRole = BearerToken(FakeUsers.superUserToken)
  val tokenRead    = BearerToken(FakeUsers.testReadToken)
  val tokenTest    = BearerToken(FakeUsers.testUserToken)

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

  val conservationReportUrl = (mid: Int, collectionId: String, EventId: Long) =>
    s"${baseUrl(mid)}/conservationReport/$EventId?collectionId=$collectionId"

  val conservationReportHTMLUrl = (mid: Int, collectionId: String, EventId: Long) =>
    s"${baseUrl(mid)}/conservationReportHTML/$EventId?collectionId=$collectionId"

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

  def getConservationReport(
      eventId: Long,
      mid: Int,
      collectionId: String,
      t: BearerToken = token
  ) = {
    wsUrl(conservationReportUrl(mid, collectionId, eventId))
      .withHttpHeaders(t.asHeader)
      .get()
      .futureValue
  }

  def getConservationReportHTML(
      eventId: Long,
      mid: Int,
      collectionId: String,
      t: BearerToken = token
  ) = {
    wsUrl(conservationReportHTMLUrl(mid, collectionId, eventId))
      .withHttpHeaders(t.asHeader)
      .get()
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

  val standaloneTreatmentId                           = 4L
  val compositeConservationProcessEventId             = standaloneTreatmentId + 1
  val treatmentId                                     = compositeConservationProcessEventId + 2 //The second child
  val treatmentIdWithActors                           = treatmentId + 2 // one spesific treatment to check for later
  val compositeConservationProcessSingleObjectEventId = 8L

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
          "2350578d-0bb0-4601-92d4-817478ad0952",
          "c182206b-530c-4a40-b9aa-fba044ecb953",
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
          "affectedThings" -> Seq("2350578d-0bb0-4601-92d4-817478ad0952"),
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
        ObjectUUID.unsafeFromString("2350578d-0bb0-4601-92d4-817478ad0952"),
        ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"),
        ObjectUUID.unsafeFromString("376d41e7-c463-45e8-9bde-7a2c9844637e")
      )

      "add composite ConservationProcess (ie with children)" in {

        val treatment1 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "note"           -> "en fin treatment",
          "affectedThings" -> Seq("c182206b-530c-4a40-b9aa-fba044ecb953"),
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
          Seq(ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"))
        )
      }

      "add composite ConservationProcess with single object (ie with children)" in {

        val treatment1 = Json.obj(
          "eventTypeId"    -> treatmentEventTypeId,
          "note"           -> "en fin treatment",
          "affectedThings" -> Seq("c182206b-530c-4a40-b9aa-fba044ecb953"),
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
          "affectedThings" -> Seq("c182206b-530c-4a40-b9aa-fba044ecb953"),
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
          "affectedThings" -> Seq("c182206b-530c-4a40-b9aa-fba044ecb953"),
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
        eventId.underlying mustBe compositeConservationProcessSingleObjectEventId

        val cpr = getEventObject(compositeConservationProcessSingleObjectEventId)
          .asInstanceOf[ConservationProcess]
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

      "get composite ConservationProcess with single object (ie with children)" in {
        val res = getEvent(compositeConservationProcessEventId)
        res.status mustBe OK
        val consProcess = res.json.validate[ConservationProcess].get
        consProcess.events.get.length must be >= 2
        consProcess.registeredBy must not be None
        //consProcess.affectedThings mustBe Some(oids)
        consProcess.affectedThings.get.length mustBe 3
        val firstEvent = consProcess.events.get.head
        firstEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"))
        )
      }

      "get Conservation Report" in {
        val res = getConservationReport(compositeConservationProcessEventId, 99, cid)
        res.status mustBe OK
        /*  val consProcess = res.json.validate[ConservationProcessForReport].get
        consProcess.events.get.length must be >= 2
        consProcess.registeredBy must not be None
        //consProcess.affectedThings mustBe Some(oids)
        consProcess.affectedThings.get.length mustBe 3
        val firstEvent = consProcess.events.get.head
        firstEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"))
        )*/
      }

      "get Conservation Report HTML" in {
        val res = getConservationReportHTML(compositeConservationProcessEventId, 99, cid)
        res.status mustBe OK
//        println(res.body)
        /*  val consProcess = res.json.validate[ConservationProcessForReport].get
        consProcess.events.get.length must be >= 2
        consProcess.registeredBy must not be None
        //consProcess.affectedThings mustBe Some(oids)
        consProcess.affectedThings.get.length mustBe 3
        val firstEvent = consProcess.events.get.head
        firstEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"))
        )*/
      }

      "get Conservation Report HTML with single object" in {
        val res = getConservationReportHTML(
          compositeConservationProcessSingleObjectEventId,
          99,
          cid
        )
        res.status mustBe OK
        /*  val consProcess = res.json.validate[ConservationProcessForReport].get
        consProcess.events.get.length must be >= 2
        consProcess.registeredBy must not be None
        //consProcess.affectedThings mustBe Some(oids)
        consProcess.affectedThings.get.length mustBe 3
        val firstEvent = consProcess.events.get.head
        firstEvent.affectedThings mustBe Some(
          Seq(ObjectUUID.unsafeFromString("c182206b-530c-4a40-b9aa-fba044ecb953"))
        )*/
      }

    }

//    "searching for filenames" should {
//      "return a list of results matching the query paramter" in {
//        val queryParam =
//          (fileIds: String) => s"/99/conservation/conservationReport/attachments/$fileIds"
//
//        val fakeToken = BearerToken(FakeUsers.testReadToken)
//        val myurl     = queryParam("096b554a-a3e6-439c-b46d-638021cb9aee")
//        println("myurl: " + myurl)
//
//        val res = wsUrl(myurl).withHttpHeaders(fakeToken.asHeader).get().futureValue
//        res.status mustBe Status.OK
//      }
//    }
  }
}
