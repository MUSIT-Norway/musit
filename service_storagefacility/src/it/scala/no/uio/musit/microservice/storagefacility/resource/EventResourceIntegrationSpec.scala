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

package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import no.uio.musit.microservice.storagefacility.domain.event.control.Control
import no.uio.musit.microservice.storagefacility.domain.storage.{EnvironmentRequirement, StorageNodeId}
import no.uio.musit.microservice.storagefacility.test.{EventJsonGenerator, StorageNodeJsonGenerator, _}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}

import scala.util.Try

class EventResourceIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  override def beforeTests(): Unit = {
    Try {
      import StorageNodeJsonGenerator._
      // Initialise some storage units...
      wsUrl(StorageNodesUrl).post(organisationJson("Foo")).futureValue
      wsUrl(StorageNodesUrl).post(buildingJson("Bar", StorageNodeId(1))).futureValue
      println("Done populating") // scalastyle:ignore
    }.recover {
      case t: Throwable =>
        println("Error occured when loading data") // scalastyle:ignore
    }
  }

  "The storage facility event service" should {

    "successfully register a new control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(20))
      val res = wsUrl(ControlsUrl(2)).post(json).futureValue

      res.status mustBe Status.CREATED
      val maybeCtrlId = (res.json \ "id").asOpt[Long]

      maybeCtrlId must not be None
    }

    "get a specific control for a node" in {
      val ctrlId = 2
      val res = wsUrl(s"${ControlsUrl(2)}/$ctrlId").get().futureValue

      res.status mustBe Status.OK

      val ctrlRes = res.json.validate[Control]
      ctrlRes.isSuccess mustBe true

      val ctrl = ctrlRes.get

      ctrl.eventType.registeredEventId mustBe ControlEventType.id
    }

    "successfully register another control" in {
      val json = Json.parse(EventJsonGenerator.controlJson(22))
      val res = wsUrl(ControlsUrl(2)).post(json).futureValue

      res.status mustBe Status.CREATED
      (res.json \ "id").asOpt[Long] must not be None
    }

    "successfully register a new observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(22))
      val res = wsUrl(ObservationsUrl(2)).post(json).futureValue

      res.status mustBe Status.CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "get a specific observation for a node" in {

    }

    "successfully register another observation" in {
      val json = Json.parse(EventJsonGenerator.observationJson(22))
      val res = wsUrl(ObservationsUrl(2)).post(json).futureValue

      res.status mustBe Status.CREATED
      val obsId = (res.json \ "id").asOpt[Long]
      obsId must not be None
    }

    "list all controls and observations for a node, ordered by doneDate" in {
      // TODO: Update this test once observations are created in above tests
      val res = wsUrl(CtrlObsForNodeUrl(2)).get().futureValue

      res.status mustBe Status.OK
      res.json.as[JsArray].value.size mustBe 4
      // TODO: Verify ordering.
    }

    //    "list all controls for a node"

  }

  "return the last environment requirement event for the node" in {
    val nodeJson = StorageNodeJsonGenerator.roomJson("test")
    val res = wsUrl(StorageNodesUrl).post(nodeJson).futureValue

    res.status mustBe Status.CREATED
    val maybeNodeId = (res.json \ "id").asOpt[Long]

    maybeNodeId must not be None

    maybeNodeId.foreach(nodeId => {
      val jsonControl = Json.parse(EventJsonGenerator.controlJson(20))
      val resCtrl = wsUrl(ControlsUrl(nodeId)).post(jsonControl).futureValue

      resCtrl.status mustBe Status.CREATED

      val resNode = wsUrl(StorageNodeUrl(nodeId)).get().futureValue

      val maybeEnvData = (resNode.json \ "environmentRequirement").asOpt[EnvironmentRequirement]

      maybeEnvData must not be None

      maybeEnvData.foreach(envData => {
        envData.temperature must not be None
        envData.temperature.foreach(temp => {
          temp.base mustBe 20
          temp.tolerance mustBe Some(25)
        })
        envData.relativeHumidity must not be None
        envData.relativeHumidity.foreach(relhum => {
          relhum.base mustBe 60.7
          relhum.tolerance mustBe Some(70)
        })
        envData.hypoxicAir must not be None
        envData.hypoxicAir.foreach(hypoxicAir => {
          hypoxicAir.base mustBe 12
          hypoxicAir.tolerance mustBe Some(20)
        })
      })
    })
  }

}