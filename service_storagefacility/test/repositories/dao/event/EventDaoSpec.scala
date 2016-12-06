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

package repositories.dao.event

import models.event.EventTypeRegistry.TopLevelEvents.{ControlEventType, MoveNodeType, MoveObjectType, ObservationEventType}
import models.event.dto._
import models.event.{ActorRole, EventType, ObjectRole, PlaceRole}
import no.uio.musit.models.{EventId, MuseumId, ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.Inspectors._
import org.scalatest.time.{Millis, Seconds, Span}
import utils.testhelpers.{EventGenerators, NodeGenerators}

/**
 * Test specs for the EventDao.
 */
class EventDaoSpec extends MusitSpecWithAppPerSuite
    with EventGenerators
    with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  // This is mutable to allow keeping track of the last inserted eventId.
  private var latestEventId: EventId = _

  "The EventDao" when {

    "processing controls with sub-controls and observations" should {
      "succeed when inserting a Control" in {
        val mid = MuseumId(2)
        val ctrl = createControl(defaultBuilding.id)
        latestEventId = addControl(mid, ctrl).futureValue

        latestEventId mustBe an[EventId]
        latestEventId mustBe EventId(1L)
      }

      "return the Control associated with the provided Id" in {
        val mid = MuseumId(2)
        val ctrl = createControl(defaultBuilding.id)
        val ctrlId = addControl(mid, ctrl).futureValue
        val res = eventDao.getEvent(mid, latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case base: BaseEventDto =>
            val c = DtoConverters.CtrlConverters.controlFromDto(base)
            c.eventType mustBe EventType.fromEventTypeId(ControlEventType.id)
            c.registeredBy mustBe Some(defaultActorId)
            c.registeredDate must not be None
            c.temperature mustBe ctrl.temperature
            c.alcohol mustBe ctrl.alcohol
            c.cleaning mustBe ctrl.cleaning
            c.pest mustBe ctrl.pest
            c.relativeHumidity mustBe ctrl.relativeHumidity
            c.mold mustBe ctrl.mold
            c.gas mustBe ctrl.gas
            c.hypoxicAir mustBe ctrl.hypoxicAir
            c.lightingCondition mustBe ctrl.lightingCondition

          case _ =>
            fail("Expected dto to be of type BaseEventDto")
        }
      }

    }

    "processing observations" should {
      "succeed when inserting an observation" in {
        val mid = MuseumId(2)
        val obs = createObservation(defaultBuilding.id)
        val eventId = addObservation(mid, obs).futureValue

        latestEventId = eventId

        eventId mustBe an[EventId]
      }

      "return the Observation associated with the provided Id" in {
        val mid = MuseumId(2)
        val obs = createObservation(defaultBuilding.id)
        val obsId = addObservation(mid, obs).futureValue
        val res = eventDao.getEvent(mid, latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case base: BaseEventDto =>
            val o = DtoConverters.ObsConverters.observationFromDto(base)

            o.eventType mustBe EventType.fromEventTypeId(ObservationEventType.id)
            o.registeredBy mustBe Some(defaultActorId)
            o.registeredDate must not be None
            o.alcohol mustBe obs.alcohol
            o.cleaning mustBe obs.cleaning
            o.gas mustBe obs.gas
            o.hypoxicAir mustBe obs.hypoxicAir
            o.lightingCondition mustBe obs.lightingCondition
            o.mold mustBe obs.mold
            o.pest mustBe obs.pest
            o.relativeHumidity mustBe o.relativeHumidity
            o.temperature mustBe obs.temperature

          case _ =>
            fail("Expected dto to be of type BaseEventDto")
        }
      }
    }

    "processing environment requirements" should {

      val envReq = createEnvRequirement(defaultBuilding.id)

      "succeed when inserting an Environment Requirement" in {
        val mid = MuseumId(2)
        val erDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)
        val eventId = eventDao.insertEvent(mid, erDto).futureValue

        latestEventId = eventId

        eventId mustBe an[EventId]
      }

      "return the Environment Requirement event with the provided ID" in {
        val mid = MuseumId(2)
        val res = eventDao.getEvent(mid, latestEventId).futureValue

        res.isFailure must not be true
        res.get.isEmpty must not be true

        res.get.get match {
          case ext: ExtendedDto =>
            val er = DtoConverters.EnvReqConverters.envReqFromDto(ext)

            er.eventType mustBe envReq.eventType
            er.note mustBe envReq.note
            er.registeredBy mustBe Some(defaultActorId)
            er.registeredDate must not be None
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
        val mid = MuseumId(2)
        val moveObj = createMoveObject(
          from = Some(StorageNodeDatabaseId(1)),
          to = StorageNodeDatabaseId(2)
        )
        val dto = DtoConverters.MoveConverters.moveObjectToDto(moveObj)
        val eventId = eventDao.insertEvent(mid, dto).futureValue

        latestEventId = eventId

        eventId mustBe an[EventId]
      }

      "return the move object event" in {
        val mid = MuseumId(2)
        val res = eventDao.getEvent(mid, latestEventId, recursive = false).futureValue
        res.isFailure must not be true
        res.get.isEmpty must not be true

        val theDto = res.get.get
        theDto mustBe a[BaseEventDto]

        val br = theDto.asInstanceOf[BaseEventDto]

        val baseRoleActor = EventRoleActor.toActorRole(br.relatedActors.head)
        val baseRolePlace = EventRolePlace.toPlaceRole(br.relatedPlaces.head)
        val baseRoleObj = EventRoleObject.toObjectRole(br.relatedObjects.head)

        br.eventTypeId mustBe MoveObjectType.id
        baseRoleActor mustBe ActorRole(1, defaultActorId)
        baseRoleObj mustBe ObjectRole(1, ObjectId(1))
        baseRolePlace mustBe PlaceRole(1, StorageNodeDatabaseId(2))
        br.valueLong mustBe Some(1L)
      }

      "succeed when moving a storage node" in {
        val mid = MuseumId(2)
        val moveNode = createMoveNode(
          from = Some(StorageNodeDatabaseId(1)),
          to = StorageNodeDatabaseId(2)
        )

        val dto = DtoConverters.MoveConverters.moveNodeToDto(moveNode)
        val eventId = eventDao.insertEvent(mid, dto).futureValue

        latestEventId = eventId

        eventId mustBe a[EventId]

      }

      "return the move node event" in {
        val mid = MuseumId(2)
        val res = eventDao.getEvent(mid, latestEventId, recursive = false).futureValue
        res.isFailure must not be true
        res.get.isEmpty must not be true

        val theDto = res.get.get
        theDto mustBe a[BaseEventDto]

        val br = theDto.asInstanceOf[BaseEventDto]

        val baseRoleActor = EventRoleActor.toActorRole(br.relatedActors.head)
        val baseRolePlace = EventRolePlace.toPlaceRole(br.relatedPlaces.head)
        val baseRoleObj = EventRoleObject.toObjectRole(br.relatedObjects.head)

        br.eventTypeId mustBe MoveNodeType.id
        baseRoleActor mustBe ActorRole(1, defaultActorId)
        baseRolePlace mustBe PlaceRole(1, StorageNodeDatabaseId(2))
        baseRoleObj mustBe ObjectRole(1, ObjectId(1))
        br.valueLong mustBe Some(1L)
      }

    }

    "fetching events for a node" should {
      "return all control events" in {
        val ctrl1 = createControl(defaultBuilding.id)
        val ctrl2 = createControl(defaultBuilding.id)
        val ctrl3 = createControl(defaultBuilding.id)

        val ctrlId1 = addControl(defaultMuseumId, ctrl1).futureValue
        val ctrlId2 = addControl(defaultMuseumId, ctrl2).futureValue
        val ctrlId3 = addControl(defaultMuseumId, ctrl3).futureValue

        val controls = eventDao.getEventsForNode(
          mid = defaultMuseumId,
          id = defaultBuilding.id.get,
          eventType = ControlEventType
        ).futureValue

        controls must not be empty
        controls.size mustBe 5

        forAll(controls) { c =>
          c.eventTypeId mustBe ControlEventType.id
          c.relatedObjects.head.objectId.underlying mustBe defaultBuilding.id.get.underlying
        }
      }

      "return all observation events" in {
        val mid = MuseumId(2)
        val obs1 = createObservation(defaultRoom.id)
        val obs2 = createObservation(defaultRoom.id)

        val obsId1 = addObservation(mid, obs1).futureValue
        val obsId2 = addObservation(mid, obs2).futureValue

        val observations = eventDao.getEventsForNode(
          mid,
          defaultRoom.id.get,
          ObservationEventType
        ).futureValue

        observations must not be empty
        observations.size mustBe 2

        forAll(observations) { o =>
          o.eventTypeId mustBe ObservationEventType.id
          o.relatedObjects.head.objectId.underlying mustBe defaultRoom.id.get.underlying
        }
      }

    }
  }
}

