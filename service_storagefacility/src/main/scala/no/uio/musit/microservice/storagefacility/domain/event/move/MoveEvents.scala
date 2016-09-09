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

package no.uio.musit.microservice.storagefacility.domain.event.move

import no.uio.musit.microservice.storagefacility.domain.event._
import play.api.libs.json.{ Format, Json }

sealed trait Move extends MusitEvent {
  val baseEvent: MusitEventBase
  val eventType: EventType
  val to: PlaceRole
}

// FIXME: Might need implicit formatters for the Move trait that resolves to specific types.

case class MoveObject(
  baseEvent: MusitEventBase,
  eventType: EventType,
  to: PlaceRole
) extends Move

object MoveObject {
  implicit val format: Format[MoveObject] = Json.format[MoveObject]
}

case class MoveNode(
  baseEvent: MusitEventBase,
  eventType: EventType,
  to: PlaceRole
) extends Move

object MoveNode {
  implicit val format: Format[MoveNode] = Json.format[MoveNode]
}
