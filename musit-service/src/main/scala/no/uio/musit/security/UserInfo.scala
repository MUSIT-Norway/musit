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

import no.uio.musit.models.ActorId
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserInfo(
  id: ActorId,
  feideEmail: Option[String],
  name: Option[String],
  email: Option[String],
  picture: Option[String]
)

object UserInfo {

  def removePrefix(str: String): String = str.reverse.takeWhile(_ != ':').reverse.trim

  implicit val format: Format[UserInfo] = (
    // format: OFF
    (__ \ "userid").format[ActorId] and
    // FIXME: Ideally we should ensure that _all_ the secondary ID's are parsed
    // And taken care of. But we need to get cracking! This _must_ be fixed.
    (__ \ "userid_sec").formatNullable[Seq[String]].inmap[Option[String]](
      mss => mss.flatMap(_.headOption.map(removePrefix)),
      ms => ms.map(s => Seq(s))
    ) and
    (__ \ "name").formatNullable[String] and
    (__ \ "email").formatNullable[String] and
    (__ \ "profilephoto").formatNullable[String]
  // format: ON
  )(UserInfo.apply, unlift(UserInfo.unapply))

}
