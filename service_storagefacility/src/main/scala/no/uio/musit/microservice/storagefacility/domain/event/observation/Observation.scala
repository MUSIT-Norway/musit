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

import no.uio.musit.microservice.storagefacility.domain.ActorId
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import org.joda.time.DateTime
import play.api.libs.json.{Format, _}

case class Observation(
  id: Option[EventId],
  doneBy: Option[ActorId],
  doneDate: DateTime,
  note: Option[String],
  affectedThing: Option[StorageNodeId],
  registeredBy: Option[String],
  registeredDate: Option[DateTime],
  eventType: EventType,
  alcohol: Option[ObservationAlcohol],
  cleaning: Option[ObservationCleaning],
  gas: Option[ObservationGas],
  hypoxicAir: Option[ObservationHypoxicAir],
  lightingCondition: Option[ObservationLightingCondition],
  mold: Option[ObservationMold],
  pest: Option[ObservationPest],
  relativeHumidity: Option[ObservationRelativeHumidity],
  temperature: Option[ObservationTemperature]
) extends MusitEvent

object Observation {
  implicit val format: Format[Observation] = Json.format[Observation]
}