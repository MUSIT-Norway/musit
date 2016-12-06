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

package models.event.observation

import models.event.observation.ObservationSubEvents._
import models.event.{EventType, MusitEvent}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, EventId, StorageNodeDatabaseId}
import org.joda.time.DateTime
import play.api.libs.json.{Format, _}

case class Observation(
  id: Option[EventId],
  doneBy: Option[ActorId],
  doneDate: DateTime,
  affectedThing: Option[StorageNodeDatabaseId],
  registeredBy: Option[ActorId],
  registeredDate: Option[DateTime],
  eventType: EventType,
  alcohol: Option[ObservationAlcohol] = None,
  cleaning: Option[ObservationCleaning] = None,
  gas: Option[ObservationGas] = None,
  hypoxicAir: Option[ObservationHypoxicAir] = None,
  lightingCondition: Option[ObservationLightingCondition] = None,
  mold: Option[ObservationMold] = None,
  pest: Option[ObservationPest] = None,
  relativeHumidity: Option[ObservationRelativeHumidity] = None,
  temperature: Option[ObservationTemperature] = None,
  theftProtection: Option[ObservationTheftProtection] = None,
  fireProtection: Option[ObservationFireProtection] = None,
  perimeterSecurity: Option[ObservationPerimeterSecurity] = None,
  waterDamageAssessment: Option[ObservationWaterDamageAssessment] = None
) extends MusitEvent

object Observation extends WithDateTimeFormatters {
  implicit val format: Format[Observation] = Json.format[Observation]
}