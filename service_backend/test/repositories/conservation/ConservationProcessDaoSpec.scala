package repositories.conservation

import java.util.UUID

import models.conservation.events.ConservationProcess
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import no.uio.musit.time.dateTimeNow
import org.scalatest.OptionValues
import play.api.libs.json.JsObject
import repositories.conservation.dao.{ConservationProcessDao, EventAccessors}
import utils.testdata.ConservationprocessGenerators

class ConservationProcessDaoSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues
    with ConservationprocessGenerators {

  private val dao = fromInstanceCache[ConservationProcessDao]

  val collections = Seq(
    MuseumCollection(
      uuid = MuseumCollections.Archeology.uuid,
      name = Some("Arkeologi"),
      oldSchemaNames = Seq(MuseumCollections.Archeology)
    )
  )

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

  def saveConservationProcess(
      oids: Option[Seq[ObjectUUID]],
      mid: MuseumId = defaultMid
  ): MusitResult[EventId] = {
    val cpe = dummyConservationProcess(oids)
    dao.insert(mid, cpe).value.futureValue
  }

  "ConservationProcessDao" when {

    "inserting conservationProcess events" should {
      "return the EventId allocated to a single conservationProcess" in {
        val oids: Seq[ObjectUUID] = Seq(oid1, oid2)
        saveConservationProcess(Some(oids)) mustBe MusitSuccess(EventId(1))
      }

      "return the conservationProcess for a spesific EventId" in {
        val res =
          dao
            .findConservationProcessIgnoreSubEvents(defaultMid, EventId(1))
            .value
            .futureValue
        res.isSuccess mustBe true
        res.successValue must not be empty

        val cp = res.successValue
        cp.value.note.value must include("SaveConservation")
        cp.value.updatedBy mustBe None
        cp.value.registeredBy must not be None
        cp.value.actorsAndRoles.isDefined mustBe true
        cp.value.actorsAndRoles.get.length mustBe 1
        cp.value.affectedThings.get.length mustBe 2
        //check that actorsAndRoles and affectedThings are removed for json column in db
        val cpRow =
          dao.getEventRowFromEventTable(cp.value.id.get).value.futureValue.successValue
        val json = EventAccessors.valJson(cpRow).asInstanceOf[JsObject]

        (json \ "affectedThings").isDefined mustBe false
        (json \ "actorsAndRoles").isDefined mustBe false
      }
    }

    "updating an conservationProcess event" should {
      "successfully save the modified fields" in {
        val eid = EventId(1L)
        val cp =
          dao
            .findConservationProcessIgnoreSubEvents(defaultMid, eid)
            .value
            .futureValue
            .successValue
            .value

        val upd = cp.copy(
          updatedBy = Some(dummyActorId),
          updatedDate = Some(dateTimeNow),
          note = Some("I was just updated"),
          actorsAndRoles = None
        )
        val resTemp = dao.update(defaultMid, eid, upd).value.futureValue.successValue
        val res = dao
          .findConservationProcessIgnoreSubEvents(defaultMid, eid)
          .value
          .futureValue
          .successValue
          .value

        res.note mustBe upd.note
        res.updatedBy mustBe Some(dummyActorId)
        res.registeredBy !== res.updatedBy
        res.updatedDate mustApproximate Some(dateTimeNow)
        res.actorsAndRoles.isDefined mustBe true
        res.actorsAndRoles.get.length mustBe 0
      }
      "fail if the conservationProcess doesn't exist" in {
        val eid  = EventId(200)
        val oids = Seq(oid1, oid2)
        val cp   = dummyConservationProcess(Some(oids)).copy(id = Some(eid))

        dao.update(defaultMid, eid, cp).value.futureValue.isFailure mustBe true
      }
    }

  }
}
