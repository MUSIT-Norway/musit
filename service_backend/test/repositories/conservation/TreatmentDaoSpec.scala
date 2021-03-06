package repositories.conservation

import java.util.UUID

import models.conservation.events.{ActorRoleDate, Treatment}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.OptionValues
import play.api.libs.json.JsObject
import play.libs.Json
import repositories.conservation.dao._

class TreatmentDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  private val dao                     = fromInstanceCache[TreatmentDao]
  private val technicalDescriptionDao = fromInstanceCache[TechnicalDescriptionDao]
  private val conservationDao         = fromInstanceCache[ConservationDao]

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
      eventTypeId = Treatment.eventTypeId,
      note = Some("hurra note"),
      /*completedBy = None,
      completedDate = None,*/
      partOf = None,
      affectedThings = oids,
      registeredBy = None,
      registeredDate = now,
      updatedBy = None,
      updatedDate = now,
      actorsAndRoles = Some(
        Seq(
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"),
            now
          )
        )
      ),
      keywords = Some(Seq(1, 2)),
      materials = Some(Seq(3, 4)),
      //documents = None
      documents = Some(
        Seq(FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"))
      ),
      isUpdated = Some(false)
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
    dao.insert(mid, cpe).value.futureValue
  }

  "TreatmentDao" when {

    "inserting treatment events" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2)

      "return the EventId allocated to a single treatment" in {
        saveTreatment(Some(oids)) mustBe MusitSuccess(EventId(1))
      }

      "return the treatment for a spesific EventId" in {
        val res =
          dao.findSpecificConservationEventById(defaultMid, EventId(1)).value.futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val tr = res.successValue.value
        tr.note.value must include("hurra note")
        tr.keywords.value mustBe Seq(1, 2)
        tr.materials.value mustBe Seq(3, 4)
        tr.affectedThings.value mustBe oids
        tr.actorsAndRoles.isDefined mustBe true
        tr.actorsAndRoles.get.length mustBe 1
        tr.documents.isDefined mustBe true
        tr.documents.get.length mustBe 1

        //check that actorsAndRoles and affectedThings are removed for json column in db
        val trt  = dao.getEventRowFromEventTable(tr.id.get).value.futureValue.successValue
        val json = EventAccessors.valJson(trt).asInstanceOf[JsObject]

        (json \ "actorsAndRoles").isDefined mustBe false
        (json \ "affectedThings").isDefined mustBe false
        (json \ "documents").isDefined mustBe false
      }
    }

    "Checking updating treatment" should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2, oid3)

      "creating and updating a new treatment" in {
        saveTreatment(Some(oids)) mustBe MusitSuccess(EventId(2))

        val res =
          dao.findSpecificConservationEventById(defaultMid, EventId(2)).value.futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty
        val event = res.successValue.value

        val newEvent =
          event.copy(
            partOf = Some(EventId(1)),
            keywords = Some(Seq(1, 2, 3, 4)),
            documents =
              Some(Seq(FileId.unsafeFromString("5a928d42-05a6-44db-adef-c6dfe588f016")))
          )

        val updatedRes = dao.update(defaultMid, EventId(2), newEvent).value.futureValue

        val updatedEvent = updatedRes.successValue.value.asInstanceOf[Treatment]

        updatedEvent.partOf mustBe Some(EventId(1))
        updatedEvent.keywords mustBe Some(Seq(1, 2, 3, 4))
        updatedEvent.documents mustBe Some(
          Seq(FileId.unsafeFromString("5a928d42-05a6-44db-adef-c6dfe588f016"))
        )
      }

      "fail on findSpecificById with wrong type" in {
        val resExp =
          intercept[Exception] {
            //Forventer egentlig IllegalStateException, men ser ut som en bug, ref:
            // https://github.com/scalatest/scalatest/issues/1172
            val res =
              technicalDescriptionDao
                .findSpecificConservationEventById(defaultMid, EventId(2))
                .value
                .futureValue
          }
      }
    }
    "Delete of subevents" should {
      "delete an events" in {
        val res =
          conservationDao
            .updateCpAndDeleteSubEvent(defaultMid, EventId(2))
            .value
            .futureValue
        res.successValue mustBe 1
        val eventNotFound =
          dao.findSpecificConservationEventById(defaultMid, EventId(2)).value.futureValue
        eventNotFound.successValue mustBe None
      }
      "delete an events that not exists" in {
        val res =
          conservationDao
            .updateCpAndDeleteSubEvent(defaultMid, EventId(666))
            .value
            .futureValue
        res.isFailure mustBe true
      }
    }
  }
}
