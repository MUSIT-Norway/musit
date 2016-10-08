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

package no.uio.musit.microservice.storagefacility.domain

import play.api.libs.json.{JsNumber, Reads, Writes, _}

case class ActorId(underlying: Long) extends MusitId

object ActorId {

  implicit val reads: Reads[ActorId] = __.read[Long].map(ActorId.apply)

  implicit val writes: Writes[ActorId] = Writes { aid =>
    JsNumber(aid.underlying)
  }

  implicit def longToActorId(l: Long): ActorId = ActorId(l)

  implicit def actorIdToLong(aid: ActorId): Long = aid.underlying

  implicit def optLongToActorId(ml: Option[Long]): Option[ActorId] = {
    ml.map(longToActorId)
  }

  implicit def optActorIdToLong(maid: Option[ActorId]): Option[Long] = {
    maid.map(actorIdToLong)
  }

}