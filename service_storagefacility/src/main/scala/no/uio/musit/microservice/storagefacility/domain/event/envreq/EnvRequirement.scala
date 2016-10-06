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

package no.uio.musit.microservice.storagefacility.domain.event.envreq

import no.uio.musit.microservice.storagefacility.DummyData
import no.uio.musit.microservice.storagefacility.domain.Interval
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.storage.{EnvironmentRequirement, StorageNodeId}
import org.joda.time.DateTime

case class EnvRequirement(
    baseEvent: BaseEvent,
    eventType: EventType,
    temperature: Option[Interval[Double]],
    airHumidity: Option[Interval[Double]],
    hypoxicAir: Option[Interval[Double]],
    cleaning: Option[String],
    light: Option[String]
) extends MusitEvent {

  def similar(er: EnvRequirement): Boolean = {
    // Compare the basic similarities of the environment requirements
    baseEvent.affectedThing == er.baseEvent.affectedThing &&
      temperature == er.temperature &&
      airHumidity == er.airHumidity &&
      hypoxicAir == er.hypoxicAir &&
      cleaning == er.cleaning &&
      light == er.light &&
      baseEvent.note == er.baseEvent.note
  }

}

object EnvRequirement {

  /**
   * Convert an EnvironmentRequirement type into an EnvRequirement event.
   *
   * @param affectedNodeId The StorageNodeId the event applies to
   * @param now            The current timestamp.
   * @param er             EnvironmentRequirement to convert
   * @param currUsr        The currently logged in user.
   * @return an EnvRequirement instance
   */
  def toEnvRequirementEvent(
    affectedNodeId: StorageNodeId,
    now: DateTime,
    er: EnvironmentRequirement
  )(implicit currUsr: String): EnvRequirement = {
    EnvRequirement(
      baseEvent = BaseEvent(
        id = None,
        // FIXME: DO NOT FORGET TO CHANGE THIS!!!
        doneBy = Some(ActorRole(1, DummyData.DummyUserId)),
        doneDate = now,
        note = er.comment,
        partOf = None,
        affectedThing = Some(ObjectRole(
          // FIXME: ObjectRole should needs to pre-exist in the DB.
          // Should probably add to the DB on app bootstrap if non-existant.
          roleId = 1,
          objectId = affectedNodeId
        )),
        registeredBy = Some(currUsr),
        registeredDate = Some(now)
      ),
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
      comment = er.baseEvent.note
    )
  }

}
