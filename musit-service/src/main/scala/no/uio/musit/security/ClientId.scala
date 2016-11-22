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

package no.uio.musit.security

import java.util.UUID

import no.uio.musit.models.MusitUUID
import play.api.libs.json.{JsString, Writes, _}

import scala.util.Try

case class ClientId(underlying: UUID) extends MusitUUID

object ClientId {

  implicit val reads: Reads[ClientId] =
    __.read[String].map(s => ClientId(UUID.fromString(s)))

  implicit val writes: Writes[ClientId] = Writes(id => JsString(id.asString))

  implicit def fromUUID(uuid: UUID): ClientId = ClientId(uuid)

  def validate(str: String): Try[UUID] = Try(UUID.fromString(str))

  def generate(): ClientId = ClientId(UUID.randomUUID())

}
