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

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.observation.{ ObservationAlcohol, ObservationCleaning, ObservationPest, ObservationTemperature }
import no.uio.musit.microservice.storagefacility.domain.event.{ EventType, MusitEventBase }
import no.uio.musit.microservice.storagefacility.domain.{ FromToDouble, LifeCycle }

trait EventGenerators {

  def createBase(str: String): MusitEventBase =
    MusitEventBase(None, Some(str), None)

  def ctrlSubWithObs(
    tpe: CtrlSubEventType,
    ok: Boolean = false
  ): ControlSubEvent = {

    tpe match {
      case CtrlAlcoholType => createAlcoholControl(ok)
      case CtrlCleaningType => createCleaningControl(ok)
      case CtrlPestType => createPestControl(ok)
      case CtrlTemperatureType => createTemperatureControl(ok)
      case _ => ???
    }
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
          number = Some(100)
        ),
        LifeCycle(
          stage = Some("new colony"),
          number = Some(4)
        )
      )
    )

}
