package controllers.storage

import java.util.UUID

import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  ControlEventType,
  ObservationEventType
}
import models.storage.event.control.Control
import models.storage.event.observation.Observation
import no.uio.musit.models.{ActorId, MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.scalatest.Inside
import play.api.libs.json.{JsArray, JsObject, JsSuccess, Json}
import play.api.test.Helpers._
import utils.testdata.EventJsonGenerator
import utils.testdata.StorageNodeJsonGenerator._

import scala.util.Try

class EventControllerIntegrationSpec extends MusitSpecWithServerPerSuite with Inside {

  val mid = MuseumId(99)

  val nodeId2 = "b56b654a-6de3-442f-97af-ca6d806cc5a6"

  val fakeToken = BearerToken(FakeUsers.testWriteToken)
  val userId    = ActorId.unsafeFromString(FakeUsers.testWriteId)
  val godToken  = BearerToken(FakeUsers.superUserToken)

  override def beforeTests(): Unit = {
    Try {
      // Initialise some storage units...
      val root = wsUrl(RootNodeUrl(mid))
        .withHeaders(godToken.asHeader)
        .post(rootJson(s"event-root-node"))
        .futureValue
      val rootId = (root.json \ "id").asOpt[StorageNodeDatabaseId]
      val org = wsUrl(StorageNodesUrl(mid))
        .withHeaders(godToken.asHeader)
        .post(organisationJson("Foo", rootId))
        .futureValue
      val orgId = (org.json \ "id").as[StorageNodeDatabaseId]
      wsUrl(StorageNodesUrl(mid))
        .withHeaders(godToken.asHeader)
        .post(buildingJson("Bar", orgId))
        .futureValue
      println("Done populating") // scalastyle:ignore
    }.recover {
      case t: Throwable =>
        println("Error occured when loading data") // scalastyle:ignore
        t.printStackTrace()
    }
  }

  "The storage facility event service" should {

    "successfully register a new control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 20))
      val res =
        wsUrl(ControlsUrl(mid, nodeId2))
          .withHeaders(fakeToken.asHeader)
          .post(json)
          .futureValue

      res.status mustBe CREATED
      val maybeCtrlId = (res.json \ "id").asOpt[Long]

      maybeCtrlId must not be None
    }

    "not allow users without WRITE access to register a new control" in {
      val token     = BearerToken(FakeUsers.testUserToken)
      val badUserId = ActorId(UUID.fromString("8efd41bb-bc58-4bbf-ac95-eea21ba9db81"))
      val json      = Json.parse(EventJsonGenerator.controlJson(badUserId, 20))
      wsUrl(ControlsUrl(mid, nodeId2))
        .withHeaders(token.asHeader)
        .post(json)
        .futureValue
        .status mustBe FORBIDDEN
    }

    "get a specific control for a node" in {
      val ctrlId = 2L
      val res = wsUrl(ControlUrl(mid, nodeId2, ctrlId))
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      val ctrlRes = res.json.validate[Control]
      inside(ctrlRes) {
        case JsSuccess(ctrl, _) =>
          ctrl.eventType.registeredEventId mustBe ControlEventType.id
      }
    }

    "not allow access to control if user doesn't have READ permission" in {
      val token  = BearerToken(FakeUsers.nhmReadToken)
      val ctrlId = 2L
      wsUrl(ControlUrl(mid, nodeId2, ctrlId))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

    "successfully register another control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 22))
      val res =
        wsUrl(ControlsUrl(mid, nodeId2))
          .withHeaders(fakeToken.asHeader)
          .post(json)
          .futureValue

      res.status mustBe CREATED
      (res.json \ "id").asOpt[Long] must not be None
    }

    "fail when a sub-control is ok and contains an observation" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 5)).as[JsObject] ++
        Json.obj(
          "cleaning" -> Json.obj(
            "ok"          -> true,
            "observation" -> EventJsonGenerator.obsStringJson("cleaning")
          )
        )

      val res =
        wsUrl(ControlsUrl(mid, nodeId2))
          .withHeaders(fakeToken.asHeader)
          .post(json)
          .futureValue
      res.status mustBe BAD_REQUEST
      res.body must include("cannot also have an observation")
    }

    "fail when a sub-control is not ok and doesn't contain an observation" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 5)).as[JsObject] ++
        Json.obj("cleaning" -> Json.obj("ok" -> false))

      val res =
        wsUrl(ControlsUrl(mid, nodeId2))
          .withHeaders(fakeToken.asHeader)
          .post(json)
          .futureValue
      res.status mustBe BAD_REQUEST
      res.body must include("must have an observation")
    }

    "successfully register a new observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(userId, 22))
      val res = wsUrl(ObservationsUrl(mid, nodeId2))
        .withHeaders(fakeToken.asHeader)
        .post(json)
        .futureValue

      res.status mustBe CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "not allow users without WRITE access to register a new observation" in {
      val token     = BearerToken(FakeUsers.testUserToken)
      val badUserId = ActorId(UUID.fromString("8efd41bb-bc58-4bbf-ac95-eea21ba9db81"))

      val json = Json.parse(EventJsonGenerator.observationJson(badUserId, 20))
      wsUrl(ObservationsUrl(mid, nodeId2))
        .withHeaders(token.asHeader)
        .post(json)
        .futureValue
        .status mustBe FORBIDDEN
    }

    "get a specific observation for a node" in {
      val obsId = 4L
      val res = wsUrl(ObservationUrl(mid, nodeId2, obsId))
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      val obsRes = res.json.validate[Observation]

      inside(obsRes) {
        case JsSuccess(obs, _) =>
          obs.eventType.registeredEventId mustBe ObservationEventType.id
      }
    }

    "not allow access to observation if user doesn't have READ permission" in {
      val token = BearerToken(FakeUsers.nhmReadToken)
      val obsId = 4L
      wsUrl(ObservationUrl(mid, nodeId2, obsId))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

    "successfully register another observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(userId, 22))
      val res = wsUrl(ObservationsUrl(mid, nodeId2))
        .withHeaders(fakeToken.asHeader)
        .post(json)
        .futureValue

      res.status mustBe CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "list all controls and observations for a node, ordered by doneDate" in {
      val res = wsUrl(CtrlObsForNodeUrl(mid, nodeId2))
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe OK
      res.json.as[JsArray].value.size mustBe 4
      // TODO: Verify ordering.
    }

    "not allow access to controls and observations if user doesn't have READ " +
      "permission" in {
      val token  = BearerToken(FakeUsers.nhmReadToken)
      val ctrlId = 2L
      wsUrl(CtrlObsForNodeUrl(mid, nodeId2))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

  }

}
