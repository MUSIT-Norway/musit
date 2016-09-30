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

package no.uio.musit.microservice.storagefacility.domain.event.observation

import no.uio.musit.microservice.storagefacility.domain.{FromToDouble, LifeCycle}
import no.uio.musit.microservice.storagefacility.domain.event.{EventType, BaseEvent, MusitSubEvent}

// TODO: Document me!!!
sealed trait ObservationSubEvent extends MusitSubEvent

// TODO: Document me!!!
sealed trait ObservationFromTo extends ObservationSubEvent {
  val range: FromToDouble
}

case class ObservationRelativeHumidity(
  baseEvent: BaseEvent,
  eventType: EventType,
  range: FromToDouble
) extends ObservationFromTo

case class ObservationTemperature(
  baseEvent: BaseEvent,
  eventType: EventType,
  range: FromToDouble
) extends ObservationFromTo

case class ObservationHypoxicAir(
  baseEvent: BaseEvent,
  eventType: EventType,
  range: FromToDouble
) extends ObservationFromTo

case class ObservationLightingCondition(
  baseEvent: BaseEvent,
  eventType: EventType,
  lightingCondition: Option[String]
) extends ObservationSubEvent

case class ObservationCleaning(
  baseEvent: BaseEvent,
  eventType: EventType,
  cleaning: Option[String]
) extends ObservationSubEvent

case class ObservationGas(
  baseEvent: BaseEvent,
  eventType: EventType,
  gas: Option[String]
) extends ObservationSubEvent

case class ObservationMold(
  baseEvent: BaseEvent,
  eventType: EventType,
  mold: Option[String]
) extends ObservationSubEvent

case class ObservationTheftProtection(
  baseEvent: BaseEvent,
  eventType: EventType,
  theftProtection: Option[String]
) extends ObservationSubEvent

case class ObservationFireProtection(
  baseEvent: BaseEvent,
  eventType: EventType,
  fireProtection: Option[String]
) extends ObservationSubEvent

case class ObservationPerimeterSecurity(
  baseEvent: BaseEvent,
  eventType: EventType,
  perimeterSecurity: Option[String]
) extends ObservationSubEvent

case class ObservationWaterDamageAssessment(
  baseEvent: BaseEvent,
  eventType: EventType,
  waterDamageAssessment: Option[String]
) extends ObservationSubEvent

case class ObservationPest(
  baseEvent: BaseEvent,
  eventType: EventType,
  identification: Option[String],
  lifecycles: Seq[LifeCycle]
) extends ObservationSubEvent

case class ObservationAlcohol(
  baseEvent: BaseEvent,
  eventType: EventType,
  condition: Option[String],
  volume: Option[Double]
) extends ObservationSubEvent
