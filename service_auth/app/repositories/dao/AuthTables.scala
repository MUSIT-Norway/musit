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

package repositories.dao

import java.util.UUID

import models.GroupId
import models.dto.GroupDto
import no.uio.musit.models.ActorId
import no.uio.musit.security.Permissions.Permission
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

private[dao] trait AuthTables {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  implicit lazy val groupIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      gid => gid.asString,
      str => ActorId(UUID.fromString(str))
    )

  implicit lazy val groupIdMapper: BaseColumnType[GroupId] =
    MappedColumnType.base[GroupId, String](
      gid => gid.asString,
      str => GroupId(UUID.fromString(str))
    )

  implicit lazy val permissionMapper: BaseColumnType[Permission] =
    MappedColumnType.base[Permission, Int](
      p => p.priority,
      i => Permission.fromInt(i)
    )

  val schema = "MUSARK_AUTH"


  class GroupTable(
    val tag: Tag
  ) extends Table[GroupDto](tag, Some(schema), "GROUP") {

    val id = column[GroupId]("GROUP_UUID", O.PrimaryKey)
    val name = column[String]("GROUP_NAME")
    val permission = column[Permission]("GROUP_PERMISSION")
    val description = column[Option[String]]("GROUP_DESCRIPTION")

    override def * = (id, name, permission, description) <> (create.tupled, destroy) // scalastyle:ignore

    def create = (
      id: GroupId,
      name: String,
      permission: Permission,
      description: Option[String]
    ) => GroupDto(id, name, permission, description)

    def destroy(dto: GroupDto) =
      Some((
        dto.id,
        dto.name,
        dto.permission,
        dto.description
        ))
  }

  type UserGroupTableType = (ActorId, GroupId)

  class UserGroupTable(
    val tag: Tag
  ) extends Table[UserGroupTableType](tag, Some(schema), "USER_GROUP") {

    val userId = column[ActorId]("USER_UUID", O.PrimaryKey)
    val groupId = column[GroupId]("GROUP_UUID", O.PrimaryKey)

    override def * = (userId, groupId) // scalastyle:ignore

  }

}
