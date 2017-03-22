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

package models

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, NamedPathElement, NodePath}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}

/**
 *
 */
case class LocationHistory(
    registeredBy: ActorId,
    registeredDate: DateTime,
    doneBy: Option[ActorId],
    doneDate: DateTime,
    from: FacilityLocation,
    to: FacilityLocation
)

object LocationHistory extends WithDateTimeFormatters {
  implicit val writes: Writes[LocationHistory] = Json.writes[LocationHistory]
}

/**
 *
 */
case class FacilityLocation(
    path: NodePath,
    pathNames: Seq[NamedPathElement]
)

object FacilityLocation {
  implicit val writes: Writes[FacilityLocation] = Json.writes[FacilityLocation]
}
