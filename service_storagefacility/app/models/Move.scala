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

import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Move[A](
    destination: StorageNodeDatabaseId,
    items: Seq[A]
)

object Move {

  implicit val storageNodeIdFormat: Format[Move[StorageNodeDatabaseId]] = (
    (__ \ "destination").format[StorageNodeDatabaseId] and
      (__ \ "items").format[Seq[StorageNodeDatabaseId]]
  )((snid, items) => Move(snid, items), m => (m.destination, m.items))

  implicit val objectIdFormat: Format[Move[ObjectId]] = (
    (__ \ "destination").format[StorageNodeDatabaseId] and
      (__ \ "items").format[Seq[ObjectId]]
  )((snid, items) => Move(snid, items), m => (m.destination, m.items))

}
