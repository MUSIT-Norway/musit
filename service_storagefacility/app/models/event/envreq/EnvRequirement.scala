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

package models.event.envreq

import models.Interval
import models.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import models.event.{EventType, MusitEvent}
import models.storage.EnvironmentRequirement
import no.uio.musit.models.{ActorId, EventId, StorageNodeDatabaseId}
import org.joda.time.DateTime

case class EnvRequirement(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    note: Option[String],
    affectedThing: Option[StorageNodeDatabaseId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    temperature: Option[Interval[Double]],
    airHumidity: Option[Interval[Double]],
    hypoxicAir: Option[Interval[Double]],
    cleaning: Option[String],
    light: Option[String]
) extends MusitEvent {

  def similar(er: EnvRequirement): Boolean = {
    // Compare the basic similarities of the environment requirements
    affectedThing == er.affectedThing &&
    temperature == er.temperature &&
    airHumidity == er.airHumidity &&
    hypoxicAir == er.hypoxicAir &&
    cleaning == er.cleaning &&
    light == er.light &&
    note == er.note
  }

}

object EnvRequirement {

  /**
   * Convert an EnvironmentRequirement type into an EnvRequirement event.
   *
   * @param doneBy         The ActorId of the currently logged in user.
   * @param affectedNodeId The StorageNodeId the event applies to
   * @param now            The current timestamp.
   * @param er             EnvironmentRequirement to convert
   * @return an EnvRequirement instance
   */
  def toEnvRequirementEvent(
      doneBy: ActorId,
      affectedNodeId: StorageNodeDatabaseId,
      now: DateTime,
      er: EnvironmentRequirement
  ): EnvRequirement = {
    EnvRequirement(
      id = None,
      doneBy = Some(doneBy),
      doneDate = now,
      note = er.comment,
      affectedThing = Some(affectedNodeId),
      registeredBy = Some(doneBy),
      registeredDate = Some(now),
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = er.temperature,
      airHumidity = er.relativeHumidity,
      hypoxicAir = er.hypoxicAir,
      cleaning = er.cleaning,
      light = er.lightingCondition
    )
  }

  def fromEnvRequirementEvent(er: EnvRequirement): EnvironmentRequirement = {
    EnvironmentRequirement(
      temperature = er.temperature,
      relativeHumidity = er.airHumidity,
      hypoxicAir = er.hypoxicAir,
      cleaning = er.cleaning,
      lightingCondition = er.light,
      comment = er.note
    )
  }

}
