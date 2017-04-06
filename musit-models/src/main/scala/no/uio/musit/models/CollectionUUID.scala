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

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes, __}

case class CollectionUUID(underlying: UUID) extends MusitUUID

object CollectionUUID extends MusitUUIDOps[CollectionUUID] {
  implicit val reads: Reads[CollectionUUID] =
    __.read[String].map(s => CollectionUUID(UUID.fromString(s)))

  implicit val writes: Writes[CollectionUUID] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): CollectionUUID = CollectionUUID(uuid)

  override def generate() = CollectionUUID(UUID.randomUUID())

}
