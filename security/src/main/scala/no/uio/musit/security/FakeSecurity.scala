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
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.extensions.PlayExtensions
import no.uio.musit.microservices.common.extensions.PlayExtensions._

/**
 * Created by jstabel on 4/15/16.
 */

class FakeSecurityInMemoryInfoProvider(userId: String) extends ConnectionInfoProvider {
  // FakeSecurityUsersAndGroups init reads a config file, so this may take some time, so we do a blocking call (Future.successful) instead of an async call Future(),
  // as the latter may get a timeout (due to reading the config file)

  def getUserInfo = Future.successful(FakeSecurityUsersAndGroups.findUser(userId).getOrFail(s"Unable to find user with Id: $userId"))
  def getUserGroups = Future.successful(FakeSecurityUsersAndGroups.groupsForUserId(userId))
  def accessToken = FakeSecurity.fakeAccessTokenPrefix + userId
}

class FakeSecurityHardcodedInfoProvider(userName: String, groupIds: Seq[String]) extends ConnectionInfoProvider {
  val userInfo = new UserInfo(userName, userName)
  val userGroups = groupIds.map(id => new GroupInfo("ad hoc in memory", id, id, Some("Fake description.")))

  def getUserInfo = Future(userInfo)
  def getUserGroups = Future(userGroups)
  def accessToken = userInfo.id
}

object FakeSecurity {

  val fakeAccessTokenPrefix = "fake-token-zab-xy-" //Must match the fake_security.json file!

  def createHardcoded(userName: String, userGroupIds: Seq[String], useCache: Boolean) = {

    Security.createSecurityConnectionFromInfoProvider(new FakeSecurityHardcodedInfoProvider(userName, userGroupIds), useCache)
  }

  def createInMemory(userId: String, useCache: Boolean) = {
    Security.createSecurityConnectionFromInfoProvider(new FakeSecurityInMemoryInfoProvider(userId), useCache)
  }
  def isFakeAccessToken(token: String) = token.startsWith(fakeAccessTokenPrefix)

  def createInMemoryFromFakeAccessToken(token: String, useCache: Boolean) = {
    if (isFakeAccessToken(token)) {
      val userId = token.substring(fakeAccessTokenPrefix.length)
      createInMemory(userId, useCache)
    } else
      Future.failed(PlayExtensions.newAuthFailed("Not a valid f access token."))
  }
}