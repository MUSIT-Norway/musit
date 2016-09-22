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

package no.uio.musit.microservice.storagefacility.dao.event

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.{ ControlEventType, MoveNodeType, MoveObjectType, ObservationEventType }
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.dto._
import no.uio.musit.microservice.storagefacility.domain.event.move._
import no.uio.musit.microservice.storagefacility.testhelpers._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.Inspectors._
import org.scalatest.time.{ Millis, Seconds, Span }

/**
 * Test specs for the EventDao.
 *
 * TODO: Add a lot more test cases!!!
 */
class EventDaoSpec extends MusitSpecWithAppPerSuite
    with EventGenerators
    with NodeGenerators {

  override val dbName: String = "event-dao-db"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  // This is mutable to allow keeping track of the last inserted eventId.
  private var latestEventId: Long = _

  "The EventDao" when {

    "processing controls with sub-controls and observations" should {
      "succeed when inserting a Control" in {
        val ctrl = createControl(defaultBuilding.id)
        latestEventId = addControl(ctrl).futureValue

        latestEventId mustBe a[java.lang.Long]
        latestEventId mustBe 1L
      }

      "return the Control associated with the provided Id" in {
        val ctrl = createControl(defaultBuilding.id)
        val ctrlId = addControl(ctrl).futureValue
        val res = eventDao.getEvent(latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case base: BaseEventDto =>
            val c = DtoConverters.CtrlConverters.controlFromDto(base)

            c.eventType mustBe EventType.fromEventTypeId(ControlEventType.id)
            c.baseEvent.note mustBe ctrl.baseEvent.note
            c.baseEvent.registeredBy mustBe Some(registeredByName)
            c.baseEvent.registeredDate must not be None
            c.parts must not be None

          // TODO: Inspect all the parts

          case _ =>
            fail("Expected dto to be of type BaseEventDto")
        }
      }

    }

    "processing observations" should {
      "succeed when inserting an observation" in {
        val obs = createObservation(defaultBuilding.id)
        val eventId = addObservation(obs).futureValue

        latestEventId = eventId

        eventId mustBe a[java.lang.Long]
      }

      "return the Observation associated with the provided Id" in {
        val obs = createObservation(defaultBuilding.id)
        val obsId = addObservation(obs).futureValue
        val res = eventDao.getEvent(latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case base: BaseEventDto =>
            val o = DtoConverters.ObsConverters.observationFromDto(base)

            o.eventType mustBe EventType.fromEventTypeId(ObservationEventType.id)
            o.baseEvent.note mustBe obs.baseEvent.note
            o.baseEvent.registeredBy mustBe Some(registeredByName)
            o.baseEvent.registeredDate must not be None
            o.parts must not be None

          case _ =>
            fail("Expected dto to be of type BaseEventDto")
        }
      }
    }

    "processing environment requirements" should {

      val envReq = createEnvRequirement(defaultBuilding.id)

      "succeed when inserting an Environment Requirement" in {
        val erDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)
        val eventId = eventDao.insertEvent(erDto).futureValue

        latestEventId = eventId

        eventId mustBe a[java.lang.Long]
      }

      "return the Environment Requirement event with the provided ID" in {
        val res = eventDao.getEvent(latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case ext: ExtendedDto =>
            val er = DtoConverters.EnvReqConverters.envReqFromDto(ext)

            er.eventType mustBe envReq.eventType
            er.baseEvent.note mustBe envReq.baseEvent.note
            er.baseEvent.registeredBy mustBe Some(registeredByName)
            er.baseEvent.registeredDate must not be None
            er.light mustBe envReq.light
            er.temperature mustBe envReq.temperature
            er.hypoxicAir mustBe envReq.hypoxicAir
            er.airHumidity mustBe envReq.airHumidity
            er.cleaning mustBe envReq.cleaning

          case _ =>
            fail("Expected dto to be of type ExtendedDto")
        }
      }

    }

    "processing Move events" should {

      "succeed when moving an object" in {
        val moveObj = MoveObject(
          baseEvent = createBase("This is a note on moving an object"),
          eventType = EventType.fromEventTypeId(MoveObjectType.id),
          to = PlaceRole(1, 1)
        )
        val dto = DtoConverters.MoveConverters.moveObjectToDto(moveObj)
        val eventId = eventDao.insertEvent(dto).futureValue

        latestEventId = eventId

        eventId mustBe a[java.lang.Long]
      }

      "succeed when moving a storage node" in {
        val moveNode = MoveNode(
          baseEvent = createBase("This is a note on moving a Node"),
          eventType = EventType.fromEventTypeId(MoveNodeType.id),
          to = PlaceRole(1, 1)
        )

        val dto = DtoConverters.MoveConverters.moveNodeToDto(moveNode)
        val eventId = eventDao.insertEvent(dto).futureValue

        latestEventId = eventId

        eventId mustBe a[java.lang.Long]

      }

      "return the move node event" in {
        val res = eventDao.getEvent(latestEventId, recursive = false).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        val theDto = res.get.get
        theDto mustBe a[BaseEventDto]

        val baseRes: BaseEventDto = theDto.asInstanceOf[BaseEventDto]

        val baseRoleActor = EventRoleActor.toActorRole(baseRes.relatedActors.head)
        val baseRolePlace = EventRolePlace.toPlaceRole(baseRes.relatedPlaces.head)
        val baseRoleObject = EventRoleObject.toObjectRole(baseRes.relatedObjects.head)

        baseRes.eventTypeId mustBe MoveNodeType.id
        baseRoleActor mustBe defaultActorRole
        baseRolePlace mustBe PlaceRole(1, 1)
        baseRoleObject mustBe ObjectRole(1, 1)
      }

    }

    "fetching events for a node" should {
      "return all control events" in {
        val ctrl1 = createControl(defaultBuilding.id)
        val ctrl2 = createControl(defaultBuilding.id)
        val ctrl3 = createControl(defaultBuilding.id)

        val ctrlId1 = addControl(ctrl1).futureValue
        val ctrlId2 = addControl(ctrl2).futureValue
        val ctrlId3 = addControl(ctrl3).futureValue

        val controls = eventDao.getEventsForNode(
          defaultBuilding.id.get,
          ControlEventType
        ).futureValue

        controls must not be empty
        controls.size mustBe 5

        forAll(controls) { c =>
          c.eventTypeId mustBe ControlEventType.id
          c.relatedObjects.head.objectId mustBe defaultBuilding.id.get.underlying
        }
      }

      "return all observation events" in {
        val obs1 = createObservation(defaultRoom.id)
        val obs2 = createObservation(defaultRoom.id)

        val obsId1 = addObservation(obs1).futureValue
        val obsId2 = addObservation(obs2).futureValue

        val observations = eventDao.getEventsForNode(
          defaultRoom.id.get,
          ObservationEventType
        ).futureValue

        observations must not be empty
        observations.size mustBe 2

        forAll(observations) { o =>
          o.eventTypeId mustBe ObservationEventType.id
          o.relatedObjects.head.objectId mustBe defaultRoom.id.get.underlying
        }
      }

    }

  }

}
