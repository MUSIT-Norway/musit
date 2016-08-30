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

package no.uio.musit.microservice.storagefacility.domain.event.control

import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.microservice.storagefacility.domain.event.{ EventType, Motivates, MusitEventBase, MusitSubEvent }

/**
 *
 */
sealed trait ControlSubEvent extends MusitSubEvent with Motivates[ObservationSubEvent] {
  val ok: Boolean
}

object ControlSubEvent

case class ControlAlcohol(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationAlcohol]
) extends ControlSubEvent

case class ControlCleaning(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationCleaning]
) extends ControlSubEvent

case class ControlGas(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationGas]
) extends ControlSubEvent

case class ControlHypoxicAir(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationHypoxicAir]
) extends ControlSubEvent

case class ControlLightingCondition(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationLightingCondition]
) extends ControlSubEvent

case class ControlMold(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationMold]
) extends ControlSubEvent

case class ControlPest(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationPest]
) extends ControlSubEvent

case class ControlRelativeHumidity(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationRelativeHumidity]
) extends ControlSubEvent

case class ControlTemperature(
  baseEvent: MusitEventBase,
  eventType: EventType,
  ok: Boolean,
  motivates: Option[ObservationTemperature]
) extends ControlSubEvent