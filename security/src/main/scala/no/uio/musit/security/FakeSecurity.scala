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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by jstabel on 4/15/16.
  */



class HardcodedFakeSecurityConnection(userInfo: UserInfo, userGroups: Seq[String]) extends SecurityConnectionBaseImp(userInfo, userGroups) {

}

object FakeSecurity {
  def createHardcoded(userName: String, userGroupIds: Seq[String]) = Future(new HardcodedFakeSecurityConnection(UserInfo(userName, userName), userGroupIds))


  def createInMemory(userId: String) = {
    val user = FakeSecurityUsersAndGroups.findUser(userId)
    user match {
      case Some(u) =>
        val userGroups = FakeSecurityUsersAndGroups.groupsIdsForUserId(u.id)
        createHardcoded(u.name, userGroups)
      case None => Future.failed(new Exception(s"Couldn't find user with ID=$userId"))
    }
  }
}