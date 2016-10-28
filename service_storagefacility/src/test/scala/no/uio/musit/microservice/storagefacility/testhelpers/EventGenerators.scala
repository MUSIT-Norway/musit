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

package no.uio.musit.microservice.storagefacility.testhelpers

import no.uio.musit.microservice.storagefacility.dao.event.EventDao
import no.uio.musit.microservice.storagefacility.domain._
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents._
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.control.ControlSubEvents._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.event.observation.ObservationSubEvents._
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.models.{ActorId, MuseumId, ObjectId, StorageNodeId}
import no.uio.musit.test.MusitSpecWithApp
import org.joda.time.DateTime

trait EventGenerators extends EventTypeInitializers {
  self: MusitSpecWithApp =>

  def eventDao: EventDao = fromInstanceCache[EventDao]

  def addControl(mid: MuseumId, ctrl: Control) = {
    val ctrlAsDto = DtoConverters.CtrlConverters.controlToDto(ctrl)
    eventDao.insertEvent(mid, ctrlAsDto)
  }

  def addObservation(mid: MuseumId, obs: Observation) = {
    val obsAsDto = DtoConverters.ObsConverters.observationToDto(obs)
    eventDao.insertEvent(mid, obsAsDto)
  }

  def addEnvRequirement(mid: MuseumId, envReq: EnvRequirement) = {
    val erAsDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)
    eventDao.insertEvent(mid, erAsDto)
  }
}

trait EventTypeInitializers {

  val registeredByName = "Darth Vader"
  val defaultActorId = ActorId(12)

  def createControl(storageNodeId: Option[StorageNodeId] = None) = {
    Control(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(ControlEventType.id),
      temperature = Some(createTemperatureControl()),
      alcohol = Some(createAlcoholControl()),
      cleaning = Some(createCleaningControl(ok = true)),
      pest = Some(createPestControl())
    )
  }

  def createObservation(storageNodeId: Option[StorageNodeId] = None) = {
    Observation(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(ObservationEventType.id),
      alcohol = Some(createAlcoholObservation),
      cleaning = Some(createCleaningObservation),
      gas = Some(createGasObservation),
      hypoxicAir = Some(createHypoxicObservation),
      lightingCondition = Some(createLightingObservation),
      mold = Some(createMoldObservation),
      pest = Some(createPestObservation),
      relativeHumidity = Some(createHumidityObservation),
      temperature = Some(createTemperatureObservation),
      theftProtection = Some(createTheftObservation),
      fireProtection = Some(createFireObservation),
      perimeterSecurity = Some(createPerimeterObservation),
      waterDamageAssessment = Some(createWaterDmgObservation)
    )
  }

  def createEnvRequirement(storageNodeId: Option[StorageNodeId] = None) = {
    EnvRequirement(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      note = Some("This is an envreq note"),
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = Some(Interval(20, Some(5))),
      airHumidity = Some(Interval(60.0, Some(10))),
      hypoxicAir = Some(Interval(0, Some(15))),
      cleaning = Some("keep it clean, dude"),
      light = Some("dim")
    )
  }

  def createTemperatureControl(ok: Boolean = false): ControlTemperature = {
    ControlTemperature(ok, if (ok) None else Some(createTemperatureObservation))
  }

  def createTemperatureObservation: ObservationTemperature = {
    ObservationTemperature(
      note = Some("This is an observation temperature note"),
      range = FromToDouble(Some(12.32), Some(24.12))
    )
  }

  def createAlcoholControl(ok: Boolean = false): ControlAlcohol =
    ControlAlcohol(ok, if (ok) None else Some(createAlcoholObservation))

  def createAlcoholObservation: ObservationAlcohol =
    ObservationAlcohol(
      note = Some("This is an observation alcohol note"),
      condition = Some("pretty strong"),
      volume = Some(92.30)
    )

  def createCleaningControl(ok: Boolean = false): ControlCleaning =
    ControlCleaning(ok, if (ok) None else Some(createCleaningObservation))

  def createCleaningObservation: ObservationCleaning =
    ObservationCleaning(
      note = Some("This is an observation cleaning note"),
      cleaning = Some("Pretty dirty stuff")
    )

  def createGasObservation: ObservationGas =
    ObservationGas(
      note = Some("This is an observation gas note"),
      gas = Some("Smells like methane")
    )

  def createHypoxicObservation: ObservationHypoxicAir =
    ObservationHypoxicAir(
      note = Some("This is an observation hypoxic air note"),
      range = FromToDouble(Some(11.11), Some(12.12))
    )

  def createLightingObservation: ObservationLightingCondition =
    ObservationLightingCondition(
      note = Some("This is an observation lighting condition note"),
      lightingCondition = Some("Quite dim")
    )

  def createMoldObservation: ObservationMold =
    ObservationMold(
      note = Some("This is an observation mold note"),
      mold = Some("Mold is a fun guy")
    )

  def createHumidityObservation: ObservationRelativeHumidity =
    ObservationRelativeHumidity(
      note = Some("This is an observation humidity note"),
      range = FromToDouble(Some(70.0), Some(75.5))
    )

  def createTheftObservation: ObservationTheftProtection =
    ObservationTheftProtection(
      note = Some("This is an observation theft note"),
      theftProtection = Some("They stole all our stuff!!")
    )

  def createFireObservation: ObservationFireProtection =
    ObservationFireProtection(
      note = Some("This is an observation fire note"),
      fireProtection = Some("Fire extinguisher is almost empty")
    )

  def createPerimeterObservation: ObservationPerimeterSecurity =
    ObservationPerimeterSecurity(
      note = Some("This is an observation perimeter note"),
      perimeterSecurity = Some("Someone has cut a hole in the fence")
    )

  def createWaterDmgObservation: ObservationWaterDamageAssessment =
    ObservationWaterDamageAssessment(
      note = Some("This is an observation water damage note"),
      waterDamageAssessment = Some("The cellar is flooded")
    )

  def createPestControl(ok: Boolean = false): ControlPest =
    ControlPest(ok, if (ok) None else Some(createPestObservation))

  def createPestObservation: ObservationPest =
    ObservationPest(
      note = Some("This is an observation pest note"),
      identification = Some("termintes"),
      lifecycles = Seq(
        LifeCycle(
          stage = Some("mature colony"),
          quantity = Some(100)
        ),
        LifeCycle(
          stage = Some("new colony"),
          quantity = Some(4)
        )
      )
    )

  def createMoveObject(
    objectId: Option[ObjectId] = Some(ObjectId(1)),
    from: Option[StorageNodeId],
    to: StorageNodeId
  ): MoveObject = {
    MoveObject(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = objectId,
      eventType = EventType.fromEventTypeId(MoveObjectType.id),
      from = from,
      to = to
    )
  }

  def createMoveNode(
    nodeId: Option[StorageNodeId] = Some(StorageNodeId(1)),
    from: Option[StorageNodeId],
    to: StorageNodeId
  ): MoveNode = {
    MoveNode(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = nodeId,
      eventType = EventType.fromEventTypeId(MoveNodeType.id),
      from = from,
      to = to
    )
  }
}
