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

import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{GroupId, GroupModule, MuseumId}
import no.uio.musit.security.Permissions.{Permission, Unspecified}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json, Reads}

import scala.util.Try

case class Group(
    id: GroupId,
    name: String,
    module: GroupModule,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String]
)

object Group {

  implicit def format: Format[Group] = Json.format[Group]

  def fromGroupAdd(gid: GroupId, ga: GroupAdd): Group =
    Group(gid, ga.name, ga.module, ga.permission, ga.museumId, ga.description)
}

case class GroupAdd(
    name: String,
    module: GroupModule,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String]
)

object GroupAdd {

  implicit def reads: Reads[GroupAdd] = Json.reads[GroupAdd]

  def applyForm(name: String, moduleId: Int, permInt: Int, mid: Int, maybeDesc: Option[String]) =
    GroupAdd(name, GroupModule.unsafeFromInt(moduleId), Permission.fromInt(permInt), MuseumId(mid), maybeDesc)

  def unapplyForm(g: GroupAdd) = Some(
    (g.name, g.module.id, g.permission.priority, g.museumId.underlying, g.description)
  )

  val groupAddForm = Form(
    mapping(
      "name"        -> text(minLength = 3),
      "module"      -> number.verifying(GroupModule.fromInt(_).nonEmpty),
      "permission"  -> number.verifying(Permission.fromInt(_) != Unspecified),
      "museum"      -> number.verifying(m => Museum.fromMuseumId(MuseumId(m)).nonEmpty),
      "description" -> optional(text)
    )(applyForm)(unapplyForm)
  )

}
