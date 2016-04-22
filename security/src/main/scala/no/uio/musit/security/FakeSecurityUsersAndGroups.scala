/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.security

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/**
  * Created by jstabel on 4/22/16.
  */




object FakeSecurityUsersAndGroups {
  val users = new ListBuffer[UserInfo]
  val groups = new ListBuffer[GroupInfo]
  val  groupsForUserMap = collection.mutable.Map[String, ListBuffer[String]]()

  def findGroup(id: String) = groups.find(_.id==id)
  def findUser(id: String) = users.find(_.id==id)

  def defUser(id: String, name: String) = {
    val user = UserInfo(id, name)
    users+=user
    user
  }

  def defGroup(id: String, displayname: String, description: String) = {
    val group = GroupInfo("ad-hoc", id, displayname, Some(description))
    groups+=group
    group
  }
  def grant(user: UserInfo, group: GroupInfo) = {
    val groupIds = groupsForUserMap.get(user.id).getOrElse(new ListBuffer[String])
    groupIds+=group.id
    groupsForUserMap.put(user.id, groupIds)
  }

  def groupsIdsForUserId(id: String) = groupsForUserMap(id)

  val jarle = defUser("jarle", "Jarle Stabell")
  val stein = defUser("stein", "Stein A. Olsen")

  val etnoLes = defGroup("EtnoLes", "EtnoLes", "Lesetilgang til etnobasen")
  val fotoLes = defGroup("FotoLes", "FotoLes", "Lesetilgang til fotobasen")

  grant(jarle, etnoLes)
  grant(jarle, fotoLes)
  grant(stein, etnoLes)
  grant(stein, fotoLes)


}
