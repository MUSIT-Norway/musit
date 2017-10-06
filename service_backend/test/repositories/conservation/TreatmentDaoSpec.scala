package repositories.conservation

import java.util.UUID

import models.conservation.events.Treatment
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.OptionValues
import repositories.conservation.dao.TreatmentDao
import utils.testdata.ConservationprocessGenerators

class TreatmentDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  private val dao = fromInstanceCache[TreatmentDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  protected val defaultMid   = Museums.Test.id
  protected val dummyActorId = ActorId.generate()

  def dummyTreatment(
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): Treatment = {
    val now = Some(dateTimeNow)
    Treatment(
      id = None,
      eventTypeId = EventTypeId(Treatment.eventTypeId),
      parentEventId = None,
      doneBy = Some(dummyActorId),
      doneDate = now,
      note = Some("hurra note"),
      affectedThing = None,
      completedBy = None,
      completedDate = None,
      caseNumber = None,
      partOf = None,
      doneByActors = None,
      affectedThings = oids,
      registeredBy = None,
      registeredDate = now,
      updatedBy = None,
      updatedDate = now,
      keywords = Some(Seq(1, 2)),
      materials = Some(Seq(3, 4))
    )
  }

  protected val oid1 =
    ObjectUUID.unsafeFromString("2e5037d5-4952-4571-9de2-709eb22b01f0")
  protected val oid2 =
    ObjectUUID.unsafeFromString("4d2e516d-db5f-478e-b409-eac7ff2486e8")
  protected val oid3 =
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

  def saveTreatment(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val cpe = dummyTreatment(oids)
    dao.insert(mid, cpe).futureValue
  }

  "TreatmentDao" when {

    "inserting treatment events" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2)

      "return the EventId allocated to a single treatment" in {
        saveTreatment(Some(oids)) mustBe MusitSuccess(EventId(1))
      }

      "return the treatment for a spesific EventId" in {
        val res = dao.findTreatmentById(defaultMid, EventId(1)).futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val tr = res.successValue.value
        tr.note.value must include("hurra note")
        tr.keywords.value mustBe Seq(1, 2)
        tr.materials.value mustBe Seq(3, 4)
        tr.affectedThings.value mustBe oids
      }
    }

    "Checking parent pointer" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2, oid3)

      "return the EventId allocated to a single treatment" in {
        saveTreatment(Some(oids)) mustBe MusitSuccess(EventId(2))

        val res = dao.findTreatmentById(defaultMid, EventId(2)).futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val event = res.successValue.value

        val newEvent =
          event.copy(parentEventId = Some(EventId(1)), keywords = Some(Seq(1, 2, 3, 4)))

        val updatedRes = dao.update(defaultMid, EventId(2), newEvent).futureValue

        val updatedEvent = updatedRes.successValue.value

        updatedEvent.parentEventId mustBe Some(EventId(1))
        updatedEvent.keywords mustBe Some(Seq(1, 2, 3, 4))
      }
    }

    /* "updating an treatment event" should {
      "successfully save the modified fields" in {
        val eid = EventId(1L)
        val cp = dao
          .findById(defaultMid, eid)
          .futureValue
          .successValue
          .value
          .asInstanceOf[Treatment]

        val upd = cp.copy(
          updatedBy = Some(dummyActorId),
          updatedDate = Some(dateTimeNow),
          note = Some("I was just updated")
        )

        val res = dao.update(defaultMid, eid, upd).futureValue.successValue.value

        res.note mustBe upd.note
        res.updatedBy mustBe Some(dummyActorId)
        res.updatedDate mustApproximate Some(dateTimeNow)
      }
      "fail if the treatment doesn't exist" in {
        val eid  = EventId(200)
        val oids = Seq(oid1, oid2)
        val cp   = dummyTreatment(Some(oids)).copy(id = Some(eid))

        dao.update(defaultMid, eid, cp).futureValue.isFailure mustBe true
      }
    }*/
  }
}
