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

package models.storage

import models.storage.event.move.MoveObject
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, NamedPathElement, NodePath, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}

case class LocationHistory(
    registeredBy: ActorId,
    registeredDate: DateTime,
    doneBy: Option[ActorId],
    doneDate: DateTime,
    id: ObjectUUID,
    objectType: ObjectType,
    from: FacilityLocation,
    to: FacilityLocation
)

object LocationHistory extends WithDateTimeFormatters {

  def fromMoveObject(
      moveObject: MoveObject,
      from: FacilityLocation,
      to: FacilityLocation
  ): LocationHistory = {
    LocationHistory(
      // registered by and date is required on Event, so they must be there.
      registeredBy = moveObject.registeredBy.get,
      registeredDate = moveObject.registeredDate.get,
      doneBy = moveObject.doneBy,
      doneDate = moveObject.doneDate,
      id = moveObject.affectedThing.get,
      objectType = moveObject.objectType,
      from = from,
      to = to
    )
  }

  implicit val writes: Writes[LocationHistory] = Json.writes[LocationHistory]
}

case class LocationHistory_Old(
    registeredBy: ActorId,
    registeredDate: DateTime,
    doneBy: Option[ActorId],
    doneDate: DateTime,
    id: Long,
    objectType: ObjectType,
    from: FacilityLocation,
    to: FacilityLocation
)

object LocationHistory_Old extends WithDateTimeFormatters {
  implicit val writes: Writes[LocationHistory_Old] = Json.writes[LocationHistory_Old]
}

case class FacilityLocation(
    path: NodePath,
    pathNames: Seq[NamedPathElement]
)

object FacilityLocation {

  def fromTuple(t: (NodePath, Seq[NamedPathElement])) = FacilityLocation(t._1, t._2)

  implicit val writes: Writes[FacilityLocation] = Json.writes[FacilityLocation]
}
