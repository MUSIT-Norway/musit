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

package no.uio.musit.models

import play.api.libs.json._

//  TODO: This class should be removed and its usage should be replaced with
//    DatabaseId everywhere in the code where. It's only temporary to allow the
//    _proper_ StorageNodeId type to be a UUID. Which is the type that _should_
//    be used to reference a StorageNode eventually

/**
 * Class to give the storage node ID a strong typing.
 */
case class StorageNodeDatabaseId(underlying: Long) extends MusitId

object StorageNodeDatabaseId {

  implicit val reads: Reads[StorageNodeDatabaseId] =
    __.read[Long].map(StorageNodeDatabaseId.apply)

  implicit val writes: Writes[StorageNodeDatabaseId] =
    Writes(id => JsNumber(id.underlying))

  implicit def fromLong(l: Long): StorageNodeDatabaseId = StorageNodeDatabaseId(l)

  implicit def toLong(id: StorageNodeDatabaseId): Long = id.underlying

  implicit def fromOptLong(l: Option[Long]): Option[StorageNodeDatabaseId] =
    l.map(fromLong)

  implicit def toOptLong(id: Option[StorageNodeDatabaseId]): Option[Long] =
    id.map(toLong)

}
