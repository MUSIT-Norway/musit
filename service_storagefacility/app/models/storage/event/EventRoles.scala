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

import no.uio.musit.models.{ActorId, ObjectId, StorageNodeDatabaseId}
import play.api.libs.json.{Format, Json}

// TODO: A better name for these

case class ActorRole(roleId: Int, actorId: ActorId)

object ActorRole {
  implicit val format: Format[ActorRole] = Json.format[ActorRole]
}

case class ObjectRole(roleId: Int, objectId: ObjectId)

object ObjectRole {
  implicit val format: Format[ObjectRole] = Json.format[ObjectRole]
}

case class PlaceRole(roleId: Int, nodeId: StorageNodeDatabaseId)

object PlaceRole {
  implicit val format: Format[PlaceRole] = Json.format[PlaceRole]
}
