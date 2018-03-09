package repositories.conservation

import java.util.UUID

import models.conservation.events._
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.libs.json.{JsObject, Json}
import repositories.conservation.dao._
import utils.testdata.ConservationprocessGenerators

class ConservationEventDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues
    with ConservationprocessGenerators {

  private val materialDeterminationDao    = fromInstanceCache[MaterialDeterminationDao]
  private val technicalDescriptionDao     = fromInstanceCache[TechnicalDescriptionDao]
  private val dao                         = fromInstanceCache[TreatmentDao]
  private val conservationDao             = fromInstanceCache[ConservationDao]
  private val measurementDeterminationDao = fromInstanceCache[MeasurementDeterminationDao]
  private val cpDao                       = fromInstanceCache[ConservationProcessDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  override val defaultMid   = Museums.Test.id
  override val dummyActorId = ActorId.generate()

  override val oid1 =
    ObjectUUID.unsafeFromString("2e5037d5-4952-4571-9de2-709eb22b01f0")
  override val oid2 =
    ObjectUUID.unsafeFromString("4d2e516d-db5f-478e-b409-eac7ff2486e8")
  override val oid3 =
    ObjectUUID.unsafeFromString("5a928d42-05a6-44db-adef-c6dfe588f016")

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(
      uuid = SessionUUID.generate(),
      oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
      userId = Option(dummyActorId),
      isLoggedIn = true
    ),
    userInfo = UserInfo(
      id = dummyActorId,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(
      GroupInfo(
        id = GroupId.generate(),
        name = "FooBarGroup",
        module = CollectionManagement,
        permission = Permissions.Admin,
        museumId = defaultMid,
        description = None,
        collections = collections
      )
    )
  )

  def dummyMaterialDeterminationEvent(
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): MaterialDetermination = {
    val actorid = ActorId.unsafeFromString("5fddd90c-bb4a-4cf8-9c27-2a9d005997bb")
    val now     = Some(dateTimeNow)
    MaterialDetermination(
      id = None,
      eventTypeId = MaterialDetermination.eventTypeId,
      registeredBy = None,
      registeredDate = now,
      updatedBy = Some(actorid),
      updatedDate = None,
      /* completedBy = None,
      completedDate = None,*/
      partOf = None,
      note = Some("hurra note"),
      actorsAndRoles = Some(
        Seq(
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"),
            now
          )
        )
      ),
      affectedThings = oids,
      documents = Some(
        Seq(FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"))
      ),
      materialInfo = Some(Seq(MaterialInfo(1, Some("veldig spes materiale"), Some(1)))),
      isUpdated = Some(false)
    )
  }

  def saveMaterialDetermination(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): FutureMusitResult[EventId] = {
    val mde = dummyMaterialDeterminationEvent(oids)
    materialDeterminationDao.insert(mid, mde)
  }

  def saveConservationProcess(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val cpe = dummyConservationProcess(oids)
    cpDao.insert(mid, cpe).value.futureValue
  }

  "ConservationEventDao" when {
    val oids: Seq[ObjectUUID] = Seq(oid1, oid2)
    "MaterialDetermination tests " should {
      "return the EventId allocated to a single materialDetermination" in {
        saveMaterialDetermination(Some(oids)).value.futureValue mustBe MusitSuccess(
          EventId(1)
        )
      }
      "return the materialDetermination with spesific attributes for a spesific EventId" in {
        val res =
          materialDeterminationDao
            .findSpecificConservationEventById(defaultMid, EventId(1))
            .value
            .futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val tr = res.successValue.value
        tr.note.value must include("hurra note")

        tr.affectedThings.value mustBe oids
        tr.actorsAndRoles.isDefined mustBe true
        tr.actorsAndRoles.get.length mustBe 1
        tr.documents.isDefined mustBe true
        tr.documents.get.length mustBe 1
        tr.materialInfo.get.length mustBe 1
        //check that actorsAndRoles and affectedThings are removed for json column in db
        val trt  = dao.getEventRowFromEventTable(tr.id.get).value.futureValue.successValue
        val json = EventAccessors.valJson(trt).asInstanceOf[JsObject]

        (json \ "actorsAndRoles").isDefined mustBe false
        (json \ "affectedThings").isDefined mustBe false
        (json \ "documents").isDefined mustBe false
        (json \ "materialInfo").isDefined mustBe false
      }
    }
    "MeasurementDetermination test" should {
      "return error when validate wrong measurementData " in {
        val res =
          measurementDeterminationDao.getMeasurementData(EventId(1)).value.futureValue
        res.isFailure mustBe true
        res.asInstanceOf[MusitError].message must include(
          "\'measurementData' is undefined"
        )
      }
    }

    "delete subEvents " should {
      "add conservationProcess with one subEvent" in {
        val oids: Seq[ObjectUUID] = Seq(oid1, oid2)
        //first make a conservationProcess
        saveConservationProcess(Some(oids)) mustBe MusitSuccess(EventId(2))
        val cpId = EventId(2L)
        val mtId = EventId(1L)
        //get materialDetermination from above test
        val mtres =
          materialDeterminationDao
            .findSpecificConservationEventById(defaultMid, EventId(1))
            .value
            .futureValue
            .successValue

        mtres.isDefined mustBe true
        val mt = mtres.get

        val almostNow = time.dateTimeNow.plusSeconds(5)
        val now       = dateTimeNow

        //insert Cp'is into subEvent's partOf and insert updatedDate and actor
        val mtupd = mt.copy(
          partOf = Some(cpId),
          isUpdated = Some(true),
          updatedBy = Some(dummyActorId),
          updatedDate = Some(almostNow)
        )
        mtupd.partOf mustBe Some(cpId)

        //get the newly made cp
        val cp =
          cpDao
            .findConservationProcessIgnoreSubEvents(defaultMid, cpId)
            .value
            .futureValue
            .successValue
            .value

        // and copy subevent into it's events-sequence
        val cpupd = cp.copy(
          updatedBy = Some(dummyActorId),
          updatedDate = Some(almostNow),
          note = Some("I was just updated"),
          isUpdated = Some(true),
          events = Some(Seq(mtupd))
        )

        cpupd.events mustBe Some(Seq(mtupd))

        //update cp with it's new subevent
        val resTemp = cpDao.update(defaultMid, cpId, cpupd).value.futureValue.successValue
// get the cp again to check for updatedDate
        val res = cpDao
          .findConservationProcessIgnoreSubEvents(defaultMid, cpId)
          .value
          .futureValue
          .successValue
          .value

        res.updatedDate must not be None
        //delete the cp's subEvent
        conservationDao
          .updateCpAndDeleteSubEvent(defaultMid, mtId)
          .value
          .futureValue
          .successValue
        //get both sub Event and cp to check if updatedDate has been updated
        val deletedSubEvent =
          dao.getEventRowFromEventTable(mtId).value.futureValue.successValue
        //deletedEvent._10 mustBe Some("ddd") // cant check on actorId because we get a new uuid each run of tests
        deletedSubEvent._6 must not be None
        deletedSubEvent._6 mustApproximate Some(dateTimeNow)
        deletedSubEvent._6 must not be Some(mtupd.updatedDate)

        val udpatedCpEvent =
          dao.getEventRowFromEventTable(cpId).value.futureValue.successValue
        //deletedEvent._10 mustBe Some("ddd") // cant check on actorId because we get a new uuid each run of tests
        udpatedCpEvent._6 must not be None
        udpatedCpEvent._6 mustApproximate Some(dateTimeNow)
        udpatedCpEvent._6 must not be Some(res.updatedDate)
      }
      "change updatedBy and updatedDate when deleted " in {
        //return the EventId allocated to a single materialDetermination"
        saveMaterialDetermination(Some(oids)).value.futureValue mustBe MusitSuccess(
          EventId(3)
        )
        val res = materialDeterminationDao
          .findSpecificConservationEventById(defaultMid, EventId(3))
          .value
          .futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val event = res.successValue.value
        event.note.value must include("hurra note")
        val eventId = event.id.get
        val beforeDeletedEvent =
          dao.getEventRowFromEventTable(eventId).value.futureValue.successValue
        conservationDao
          .updateCpAndDeleteSubEvent(99, eventId)
          .value
          .futureValue
          .successValue
        // Thread.sleep(1002)
        val deletedEvent =
          dao.getEventRowFromEventTable(eventId).value.futureValue.successValue
        //deletedEvent._10 mustBe Some("ddd") // cant check on actorId because we get a new uuid each run of tests
        deletedEvent._6 must not be None
        deletedEvent._6 mustApproximate Some(dateTimeNow)
      }
    }
  }
}
