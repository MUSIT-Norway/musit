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
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.ControlSubEvents.{ CtrlAlcoholType, CtrlCleaningType, CtrlPestType, CtrlTemperatureType }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.ObservationSubEvents.{ ObsAlcoholType, ObsCleaningType, ObsPestType, ObsTemperatureType }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.{ ControlEventType, EnvRequirementEventType, ObservationEventType }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import no.uio.musit.microservice.storagefacility.domain.{ FromToDouble, Interval, LifeCycle }
import no.uio.musit.test.MusitSpecWithApp
import org.joda.time.DateTime
import play.api.Application

trait EventGenerators {
  self: MusitSpecWithApp =>

  def eventDao: EventDao = {
    val instance = Application.instanceCache[EventDao]
    instance(musitFakeApp)
  }

  val registeredByName = "Darth Vader"
  val defaultActorRole = ActorRole(1, 12)

  def addControl(ctrl: Control) = {
    val ctrlAsDto = DtoConverters.CtrlConverters.controlToDto(ctrl)
    eventDao.insertEvent(ctrlAsDto)
  }

  def addObservation(obs: Observation) = {
    val obsAsDto = DtoConverters.ObsConverters.observationToDto(obs)
    eventDao.insertEvent(obsAsDto)
  }

  def addEnvRequirement(envReq: EnvRequirement) = {
    val erAsDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)
    eventDao.insertEvent(erAsDto)
  }

  def createBase(str: String, affected: Option[Long] = Some(1)): MusitEventBase =
    MusitEventBase(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      note = Some(str),
      partOf = None,
      registeredBy = Some(registeredByName),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorRole),
      affectedThing = affected.map(a => ObjectRole(1, a))
    )

  def createControl(storageNodeId: Option[StorageNodeId] = None) = {
    Control(
      baseEvent = createBase("This is a control note", storageNodeId),
      eventType = EventType.fromEventTypeId(ControlEventType.id),
      parts = Some(Seq(
        createTemperatureControl(),
        createAlcoholControl(),
        createCleaningControl(ok = true),
        createPestControl()
      ))
    )
  }

  def createObservation(storageNodeId: Option[StorageNodeId] = None) = {
    Observation(
      baseEvent = createBase("This is an observation note", storageNodeId),
      eventType = EventType.fromEventTypeId(ObservationEventType.id),
      parts = Some(Seq(
        createCleaningObservation,
        createTemperatureObservation
      ))
    )
  }

  def createEnvRequirement(storageNodeId: Option[StorageNodeId] = None) = {
    EnvRequirement(
      baseEvent = createBase("This is the base note", storageNodeId),
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = Some(Interval(20, Some(5))),
      airHumidity = Some(Interval(60, Some(10))),
      hypoxicAir = Some(Interval(0, Some(15))),
      cleaning = Some("keep it clean, dude"),
      light = Some("dim")
    )
  }

  def createTemperatureControl(ok: Boolean = false): ControlTemperature = {
    ControlTemperature(
      baseEvent = createBase("This is a ctrl temp note"),
      eventType = EventType.fromEventTypeId(CtrlTemperatureType.id),
      ok = false,
      motivates = if (ok) None else Some(createTemperatureObservation)
    )
  }

  def createTemperatureObservation: ObservationTemperature = {
    ObservationTemperature(
      baseEvent = createBase("This is an obs temp note"),
      eventType = EventType.fromEventTypeId(ObsTemperatureType.id),
      range = FromToDouble(Some(12.32), Some(24.12))
    )
  }

  def createAlcoholControl(ok: Boolean = false): ControlAlcohol =
    ControlAlcohol(
      baseEvent = createBase("This is a ctrl alcohol note"),
      eventType = EventType.fromEventTypeId(CtrlAlcoholType.id),
      ok = ok,
      motivates = if (ok) None else Some(createAlcoholObservation)
    )

  def createAlcoholObservation: ObservationAlcohol =
    ObservationAlcohol(
      baseEvent = createBase("This is an obs alcohol note"),
      eventType = EventType.fromEventTypeId(ObsAlcoholType.id),
      condition = Some("pretty strong"),
      volume = Some(92.30)
    )

  def createCleaningControl(ok: Boolean = false): ControlCleaning =
    ControlCleaning(
      baseEvent = createBase("This is a ctrl cleaning note"),
      eventType = EventType.fromEventTypeId(CtrlCleaningType.id),
      ok = ok,
      motivates = if (ok) None else Some(createCleaningObservation)
    )

  def createCleaningObservation: ObservationCleaning =
    ObservationCleaning(
      baseEvent = createBase("This is an obs cleaning note"),
      eventType = EventType.fromEventTypeId(ObsCleaningType.id),
      cleaning = Some("Pretty dirty stuff")
    )

  def createPestControl(ok: Boolean = false): ControlPest =
    ControlPest(
      baseEvent = createBase("This is a ctrl pest note"),
      eventType = EventType.fromEventTypeId(CtrlPestType.id),
      ok = ok,
      motivates = if (ok) None else Some(createPestObservation)
    )

  def createPestObservation: ObservationPest =
    ObservationPest(
      baseEvent = createBase("This is an obs pests note"),
      eventType = EventType.fromEventTypeId(ObsPestType.id),
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

}
