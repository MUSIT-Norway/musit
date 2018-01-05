package repositories.conservation

import java.util.UUID

import models.conservation.events._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.OptionValues
import play.api.libs.json.{JsObject, Json}
import repositories.conservation.dao._

import scala.collection.script.Include

class ConservationEventDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  private val materialDeterminationDao    = fromInstanceCache[MaterialDeterminationDao]
  private val technicalDescriptionDao     = fromInstanceCache[TechnicalDescriptionDao]
  private val dao                         = fromInstanceCache[TreatmentDao]
  private val conservationDao             = fromInstanceCache[ConservationDao]
  private val measurementDeterminationDao = fromInstanceCache[MeasurementDeterminationDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

  protected val defaultMid   = Museums.Test.id
  protected val dummyActorId = ActorId.generate()

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

  def dummyMaterialDetermination(
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): MaterialDetermination = {
    val now = Some(dateTimeNow)
    MaterialDetermination(
      id = None,
      eventTypeId = MaterialDetermination.eventTypeId,
      registeredBy = None,
      registeredDate = now,
      updatedBy = None,
      updatedDate = now,
      completedBy = None,
      completedDate = None,
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
      materialInfo = Some(Seq(MaterialInfo(1, Some("veldig spes materiale"), Some(1))))
    )
  }

  def saveMaterialDetermination(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): FutureMusitResult[EventId] = {
    val mde = dummyMaterialDetermination(oids)
    materialDeterminationDao.insert(mid, mde)
  }

  "ConservationEventDao" when {

    "MaterialDetermination tests " should {
      val oids: Seq[ObjectUUID] = Seq(oid1, oid2)

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
  }
}
