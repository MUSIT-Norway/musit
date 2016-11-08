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

import play.api.libs.json.{JsNumber, Reads, Writes, _}

case class OrgId(underlying: Long) extends MusitId

object OrgId {
  implicit val reads: Reads[OrgId] = __.read[Long].map(OrgId.apply)

  implicit val writes: Writes[OrgId] = Writes(oid => JsNumber(oid.underlying))

  implicit def fromLong(l: Long): OrgId = OrgId(l)

  implicit def toLong(oid: OrgId): Long = oid.underlying

  implicit def fromOptLong(ml: Option[Long]): Option[OrgId] = ml.map(fromLong)

  implicit def toOptLong(moid: Option[OrgId]): Option[Long] = moid.map(toLong)
}
