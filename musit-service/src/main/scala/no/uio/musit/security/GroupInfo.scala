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

import no.uio.musit.models.Museums.Museum
import no.uio.musit.security.Roles.Role
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GroupInfo(
    id: String,
    groupType: String,
    displayName: String,
    description: Option[String]
) {

  val group: Option[Role] = Roles.fromGroupId(id)
  val museum: Option[Museum] = group.map(_.museum)

}

object GroupInfo {

  implicit val formats: Format[GroupInfo] = (
    (__ \ "id").format[String] and
    (__ \ "type").format[String] and
    (__ \ "displayName").format[String] and
    (__ \ "description").formatNullable[String]
  )(GroupInfo.apply, unlift(GroupInfo.unapply))

}