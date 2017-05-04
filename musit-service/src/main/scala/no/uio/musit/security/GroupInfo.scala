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
import no.uio.musit.models.{GroupId, GroupModule, MuseumCollection, MuseumId}
import no.uio.musit.security.Permissions.Permission
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GroupInfo(
    id: GroupId,
    name: String,
    module: GroupModule,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String],
    collections: Seq[MuseumCollection]
) {

  val museum: Option[Museum] = Museum.fromMuseumId(museumId)

  def hasPermission(p: Permission): Boolean = permission == p

}

object GroupInfo {

  implicit val formats: Format[GroupInfo] = (
    (__ \ "id").format[GroupId] and
      (__ \ "name").format[String] and
      (__ \ "module").format[GroupModule] and
      (__ \ "permission").format[Permission] and
      (__ \ "museumId").format[MuseumId] and
      (__ \ "description").formatNullable[String] and
      (__ \ "collections").format[Seq[MuseumCollection]]
  )(GroupInfo.apply, unlift(GroupInfo.unapply))

  def fromTuple(
    t: (GroupId, String, GroupModule, Permission, MuseumId, Option[String])
  ): GroupInfo = {
    GroupInfo(
      id = t._1,
      name = t._2,
      module = t._3,
      permission = t._4,
      museumId = t._5,
      description = t._6,
      collections = Seq.empty
    )
  }

}
