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

import org.joda.time.DateTime
import play.api.libs.json._

// TODO: Change id and partOf to EventId

case class BaseEvent(
  id: Option[Long],
  doneBy: Option[ActorRole],
  doneDate: DateTime,
  note: Option[String],
  partOf: Option[Long],
  // TODO: Move affectedThing property to the specific types?!?!?
  affectedThing: Option[ObjectRole],
  /*
    TODO: The following 2 fields are not really Optional. And is not something
    the clients of the API will send in.
    This highlights the difference between internal/external representation
    (API vs Domain). And shows the need to have a separate layer of API models
    that represent the in/out messages.
   */
  registeredBy: Option[String], // This should be UserId,
  registeredDate: Option[DateTime]
)

object BaseEvent {
  implicit val format: Format[BaseEvent] = Json.format[BaseEvent]
}