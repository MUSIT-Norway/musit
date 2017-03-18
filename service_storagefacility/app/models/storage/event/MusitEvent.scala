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

package models.storage.event

import no.uio.musit.models.{ActorId, EventId, MusitId}
import org.joda.time.DateTime

/**
 * Top level representation of _all_ event types with definitions for the
 * shared attributes they all contain.
 */
trait MusitEvent {
  val id: Option[EventId]
  val doneBy: Option[ActorId]
  val doneDate: DateTime
  val affectedThing: Option[MusitId]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val eventType: EventType
}

/**
 * Helps to identify events that are only valid in a "sub-event" context.
 */
trait MusitSubEvent
