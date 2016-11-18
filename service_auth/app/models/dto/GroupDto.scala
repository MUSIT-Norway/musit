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

package models.dto

import models.{Group, GroupAdd, GroupId}
import no.uio.musit.security.Permissions.Permission

case class GroupDto(
  id: GroupId,
  name: String,
  permission: Permission,
  description: Option[String]
)

object GroupDto {

  def fromGroupAdd(ga: GroupAdd): GroupDto = {
    GroupDto(GroupId.generate(), ga.name, ga.permission, ga.description)
  }

  def fromGroup(g: Group): GroupDto = {
    GroupDto(g.id, g.name, g.permission, g.description)
  }

  def toGroup(dto: GroupDto): Group = {
    Group(dto.id, dto.name, dto.permission, dto.description)
  }

}
