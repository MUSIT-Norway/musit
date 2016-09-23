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

package no.uio.musit.microservice.storagefacility.domain.event

/**
 * Top level representation of _all_ event types with definitions for the
 * shared attributes they all contain.
 */
trait MusitEvent {
  val baseEvent: BaseEvent
  val eventType: EventType
}

/**
 * Helps to identify events that are only valid in a "sub-event" context.
 */
trait MusitSubEvent extends MusitEvent

/**
 * Specifies a "part of" relationship on an implementation of MusitEvent.
 *
 * @tparam A the type of MusitEvent to expect in the "part of" relationship
 */
trait Parts[A <: MusitEvent] {
  val parts: Option[Seq[A]]
}

/**
 * Specifies a "motivates" relationship on an implementation of MusitEvent.
 *
 * @tparam A the type of MusitEvent to expect in the "motivates" relationship.
 */
trait Motivates[A <: MusitEvent] {
  val motivates: Option[A]
}