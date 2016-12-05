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

import no.uio.musit.models.{CollectionUUID, GroupId}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json}

/**
 * Command message for adding a user to an auth group with access to
 * specified museum collections in the form of CollectionUUID's.
 */
case class UserAuthAdd(
  email: String,
  groupId: String,
  collections: Option[List[CollectionUUID]]
)

object UserAuthAdd {
  implicit val formats: Format[UserAuthAdd] = Json.format[UserAuthAdd]

  def applyForm(
    email: String,
    groupId: String,
    collections: Option[List[CollectionUUID]]
  ) = UserAuthAdd(email, groupId, collections)

  def unapplyForm(uga: UserAuthAdd) = Some((uga.email, uga.groupId, uga.collections))

  val userAuthAddForm = Form(
    mapping(
      "email" -> email,
      "groupId" -> text.verifying(id => GroupId.validate(id).isSuccess),
      "collections" -> optional(list(uuid).transform[List[CollectionUUID]](
        _.map(CollectionUUID.apply),
        _.map(_.underlying)
      ))
    )(applyForm)(unapplyForm)
  )
}