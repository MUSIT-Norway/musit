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

import models.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import models.event.control.Control
import no.uio.musit.models.{MuseumId, StorageNodeId}
import no.uio.musit.security.{BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import utils.testhelpers.StorageNodeJsonGenerator._
import utils.testhelpers.{EventJsonGenerator, _}

import scala.util.Try

class EventResourceIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val mid = MuseumId(1)

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  override def beforeTests(): Unit = {
    Try {
      // Initialise some storage units...
      val root = wsUrl(RootNodeUrl(mid))
        .withHeaders(fakeToken.asHeader)
        .post(JsNull).futureValue
      val rootId = (root.json \ "id").asOpt[StorageNodeId]
      val org = wsUrl(StorageNodesUrl(mid))
        .withHeaders(fakeToken.asHeader)
        .post(organisationJson("Foo", rootId)).futureValue
      val orgId = (org.json \ "id").as[StorageNodeId]
      wsUrl(StorageNodesUrl(mid))
        .withHeaders(fakeToken.asHeader)
        .post(buildingJson("Bar", orgId)).futureValue
      println("Done populating") // scalastyle:ignore
    }.recover {
      case t: Throwable =>
        println("Error occured when loading data") // scalastyle:ignore
    }
  }

  "The storage facility event service" should {

    "successfully register a new control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(20))
      val res = wsUrl(ControlsUrl(mid, 2)).withHeaders(fakeToken.asHeader).post(json).futureValue

      res.status mustBe Status.CREATED
      val maybeCtrlId = (res.json \ "id").asOpt[Long]

      maybeCtrlId must not be None
    }

    "get a specific control for a node" in {
      val ctrlId = 2
      val res = wsUrl(s"${ControlsUrl(mid, 2)}/$ctrlId")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue

      res.status mustBe Status.OK

      val ctrlRes = res.json.validate[Control]
      ctrlRes.isSuccess mustBe true

      val ctrl = ctrlRes.get

      ctrl.eventType.registeredEventId mustBe ControlEventType.id
    }

    "successfully register another control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(22))
      val res = wsUrl(ControlsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json).futureValue

      res.status mustBe Status.CREATED
      (res.json \ "id").asOpt[Long] must not be None
    }

    "fail when a sub-control is ok and contains an observation" in {
      val json = Json.parse(EventJsonGenerator.controlJson(5)).as[JsObject] ++
        Json.obj(
          "cleaning" -> Json.obj(
            "ok" -> true,
            "observation" -> EventJsonGenerator.obsStringJson("cleaning")
          )
        )

      val res = wsUrl(ControlsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json).futureValue
      res.status mustBe Status.BAD_REQUEST
      res.body must include("cannot also have an observation")
    }

    "fail when a sub-control is not ok and doesn't contain an observation" in {
      val json = Json.parse(EventJsonGenerator.controlJson(5)).as[JsObject] ++
        Json.obj("cleaning" -> Json.obj("ok" -> false))

      val res = wsUrl(ControlsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json).futureValue
      res.status mustBe Status.BAD_REQUEST
      res.body must include("must have an observation")
    }

    "successfully register a new observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(22))
      val res = wsUrl(ObservationsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json).futureValue

      res.status mustBe Status.CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "get a specific observation for a node" in {
      pending
    }

    "successfully register another observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(22))
      val res = wsUrl(ObservationsUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .post(json).futureValue

      res.status mustBe Status.CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "list all controls and observations for a node, ordered by doneDate" in {
      // TODO: Update this test once observations are created in above tests
      val res = wsUrl(CtrlObsForNodeUrl(mid, 2))
        .withHeaders(fakeToken.asHeader)
        .get().futureValue

      res.status mustBe Status.OK
      res.json.as[JsArray].value.size mustBe 4
      // TODO: Verify ordering.
    }

  }

}