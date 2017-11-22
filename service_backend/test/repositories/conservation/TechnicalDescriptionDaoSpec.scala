package repositories.conservation

import java.util.UUID

import models.conservation.events.TechnicalDescription
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.OptionValues
import repositories.conservation.dao.{TechnicalDescriptionDao, TreatmentDao}

class TechnicalDescriptionDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  private val dao          = fromInstanceCache[TechnicalDescriptionDao]
  private val treatmentDao = fromInstanceCache[TreatmentDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  protected val defaultMid   = Museums.Test.id
  protected val dummyActorId = ActorId.generate()

  def dummyTechnicalDescription(
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): TechnicalDescription = {
    val now = Some(dateTimeNow)
    TechnicalDescription(
      id = None,
      eventTypeId = TechnicalDescription.eventTypeId,
      doneBy = Some(dummyActorId),
      doneDate = now,
      note = Some("eksempel på teknisk beskrivelse"),
      affectedThing = None,
      completedBy = None,
      completedDate = None,
      caseNumber = None,
      partOf = None,
      actorsAndRoles = None,
      affectedThings = oids,
      registeredBy = None,
      registeredDate = now,
      updatedBy = None,
      updatedDate = now
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

  def saveTechnicalDescription(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val cpe = dummyTechnicalDescription(oids)
    dao.insert(mid, cpe).value.futureValue
  }

  "TechnicalDescriptionDao" when {

    "inserting technical description events" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2)

      "return the EventId allocated to a single technical description" in {
        saveTechnicalDescription(Some(oids)) mustBe MusitSuccess(EventId(1))
      }

      "return the technical description for a spesific EventId" in {
        val res =
          dao.findSpecificConservationEventById(defaultMid, EventId(1)).value.futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val tr = res.successValue.value
        tr.note.value must include("eksempel på teknisk beskrivelse")
        tr.affectedThings.value mustBe oids
      }
    }

    "Checking updating technical description" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2, oid3)

      "creating and updating a new technical description" in {
        saveTechnicalDescription(Some(oids)) mustBe MusitSuccess(EventId(2))

        val res =
          dao.findSpecificConservationEventById(defaultMid, EventId(2)).value.futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val event = res.successValue.value

        val newEvent =
          event.copy(partOf = Some(EventId(1)), note = Some("New note"))

        val updatedRes =
          dao.update(defaultMid, EventId(2), newEvent).value.futureValue

        val updatedEvent =
          updatedRes.successValue.value.asInstanceOf[TechnicalDescription]

        updatedEvent.partOf mustBe Some(EventId(1))
        updatedEvent.note mustBe Some("New note")

      }

      "fail on findSpecificById with wrong type" in {
        val resExp =
          intercept[Exception] {
            //Forventer egentlig IllegalStateException, men ser ut som en bug, ref:
            // https://github.com/scalatest/scalatest/issues/1172
            val res =
              treatmentDao
                .findSpecificConservationEventById(defaultMid, EventId(2))
                .value
                .futureValue
          }
      }
    }
  }
}
