/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package controllers

import java.util.UUID

import models.event.EventTypeRegistry.TopLevelEvents.{
  ControlEventType,
  ObservationEventType
}
import models.event.control.Control
import models.event.observation.Observation
import no.uio.musit.models.{ActorId, MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.BearerToken
import no.uio.musit.security.fake.FakeAuthenticator.fakeAccessTokenPrefix
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.test.Helpers._
import utils.testhelpers.StorageNodeJsonGenerator._
import utils.testhelpers.{EventJsonGenerator, _}

import scala.util.Try

class EventControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val mid = MuseumId(99)

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
        wsUrl(ControlsUrl(mid, 2)).withHeaders(fakeToken.asHeader).post(json).futureValue

      res.status mustBe CREATED
      val maybeCtrlId = (res.json \ "id").asOpt[Long]

      maybeCtrlId must not be None
    }

    "not allow users without WRITE access to register a new control" in {
      val token     = BearerToken(FakeUsers.testUserToken)
      val badUserId = ActorId(UUID.fromString("8efd41bb-bc58-4bbf-ac95-eea21ba9db81"))
      val json      = Json.parse(EventJsonGenerator.controlJson(badUserId, 20))
      wsUrl(ControlsUrl(mid, 2))
        .withHeaders(token.asHeader)
        .post(json)
        .futureValue
        .status mustBe FORBIDDEN
    }

    "get a specific control for a node" in {
      val ctrlId = 2L
      val res = wsUrl(ControlUrl(mid, 2, ctrlId))
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      val ctrlRes = res.json.validate[Control]
      ctrlRes.isSuccess mustBe true

      val ctrl = ctrlRes.get

      ctrl.eventType.registeredEventId mustBe ControlEventType.id
    }

    "not allow access to control if user doesn't have READ permission" in {
      val token  = BearerToken(FakeUsers.nhmReadToken)
      val ctrlId = 2L
      wsUrl(ControlUrl(mid, 2, ctrlId))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

    "successfully register another control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 22))
      val res =
        wsUrl(ControlsUrl(mid, 2)).withHeaders(fakeToken.asHeader).post(json).futureValue

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
        wsUrl(ControlsUrl(mid, 2)).withHeaders(fakeToken.asHeader).post(json).futureValue
      res.status mustBe BAD_REQUEST
      res.body must include("cannot also have an observation")
    }

    "fail when a sub-control is not ok and doesn't contain an observation" in {
      val json = Json.parse(EventJsonGenerator.controlJson(userId, 5)).as[JsObject] ++
        Json.obj("cleaning" -> Json.obj("ok" -> false))

      val res =
        wsUrl(ControlsUrl(mid, 2)).withHeaders(fakeToken.asHeader).post(json).futureValue
      res.status mustBe BAD_REQUEST
      res.body must include("must have an observation")
    }

    "successfully register a new observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(userId, 22))
      val res = wsUrl(ObservationsUrl(mid, 2))
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
      wsUrl(ObservationsUrl(mid, 2))
        .withHeaders(token.asHeader)
        .post(json)
        .futureValue
        .status mustBe FORBIDDEN
    }

    "get a specific observation for a node" in {
      val obsId = 20L
      val res = wsUrl(ObservationUrl(mid, 2, obsId))
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      val obsRes = res.json.validate[Observation]
      obsRes.isSuccess mustBe true

      val obs = obsRes.get

      obs.eventType.registeredEventId mustBe ObservationEventType.id
    }

    "not allow access to observation if user doesn't have READ permission" in {
      val token = BearerToken(FakeUsers.nhmReadToken)
      val obsId = 20L
      wsUrl(ObservationUrl(mid, 2, obsId))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

    "successfully register another observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(userId, 22))
      val res = wsUrl(ObservationsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json)
        .futureValue

      res.status mustBe CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "list all controls and observations for a node, ordered by doneDate" in {
      val res = wsUrl(CtrlObsForNodeUrl(mid, 2))
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
      val ctrlId = 2
      wsUrl(CtrlObsForNodeUrl(mid, 2))
        .withHeaders(token.asHeader)
        .get()
        .futureValue
        .status mustBe FORBIDDEN
    }

  }

}
