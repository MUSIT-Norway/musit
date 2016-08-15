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

//Domain model

package no.uio.musit.security

import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, _ }
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.security.dataporten.Dataporten
import play.api.mvc.Request

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * Created by jstabel on 4/15/16.
 */

case class UserInfo(id: String, name: String)

case class GroupInfo(groupType: String, id: String, displayName: String, description: Option[String])

//type TokenToUserIdProvider =  (String) => String

/**
 * Represents what a "connection" is expected to know of the current user
 *
 */
trait ConnectionInfoProvider {
  def getUserInfo: Future[UserInfo]

  def getUserGroups: Future[Seq[GroupInfo]]

  def getUserGroupIds: Future[Seq[String]] = getUserGroups.map(groupInfos => groupInfos.map(groupInfo => groupInfo.id))

  def accessToken: String
}

/*Not used, at least yet
trait GroupInfoProvider {
  def getGroupInfo(groupid: String): Future[Option[GroupInfo]]
}

trait UserInfoProvider {
  def getUserInfo(userid: String): Future[Option[UserInfo]]
  def getUserGroups(userid: String): Future[Option[Seq[String]]]
}

//TODO? def getGroupInfo(groupid: String) : Future[Option[GroupInfo]]
*/

trait SecurityState {
  def userInfo: UserInfo

  def hasGroup(group: String): Boolean

  def hasAllGroups(groups: Seq[String]): Boolean

  def hasNoneOfGroups(groups: Seq[String]): Boolean
}

trait SecurityConnection {
  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T]

  def state: SecurityState

  def userName: String = state.userInfo.name

  def userId: String = state.userInfo.id

  def hasGroup(groupid: String): Boolean = state.hasGroup(groupid)

  def hasAllGroups(groupIds: Seq[String]): Boolean = state.hasAllGroups(groupIds)

  def hasNoneOfGroups(groupIds: Seq[String]): Boolean = state.hasNoneOfGroups(groupIds)

  def groupIds: Seq[String] //We could provide a default implementation as infoProvider.getUserGroupIds, but then we would have to return a Future.
  //Since all current implementations caches in the groupsIds at startup, we have a direct access here.

  ///The infoProvider providing the info to this connection. Accessing this is probably only relevant for testing/debugging
  def infoProvider: ConnectionInfoProvider
}

class SecurityStateImp(_userInfo: UserInfo, userGroups: Seq[String]) extends SecurityState {

  import no.uio.musit.microservices.common.extensions.SeqExtensions._

  override def userInfo: UserInfo = _userInfo

  def hasGroup(group: String) = userGroups.contains(group)

  def hasAllGroups(groups: Seq[String]) = userGroups.hasAllOf(groups)

  def hasNoneOfGroups(groups: Seq[String]) = userGroups.hasNoneOf(groups)
}

class SecurityConnectionImp(_infoProvider: ConnectionInfoProvider, userInfo: UserInfo, userGroups: Seq[String]) extends SecurityConnection {
  val state = new SecurityStateImp(userInfo, userGroups)

  override def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T] = {
    if (state.hasAllGroups(requiredGroups) && state.hasNoneOfGroups(deniedGroups)) {
      Success(body)
    } else {

      val missingGroups = requiredGroups.filter((g => !(state.hasGroup(g))))
      val disallowedGroups = deniedGroups.filter(g => (state.hasGroup(g)))

      assert(missingGroups.length > 0 || disallowedGroups.length > 0)

      val missingGroupsText = Some(s"Missing groups: ${missingGroups.mkString(",")}.").filter(_ => !missingGroups.isEmpty)
      val shouldNotHaveGroupsText = Some(s"Having disallowed groups: ${disallowedGroups.mkString(",")}.").filter(_ => !disallowedGroups.isEmpty)

      val msg = s"Unauthorized! ${missingGroupsText.map(_ + " ").getOrElse("")}${shouldNotHaveGroupsText.getOrElse()}"

      Failure(new Exception(msg))
    }
  }

  override def groupIds = userGroups

  def infoProvider: ConnectionInfoProvider = _infoProvider
}

object Security {
  ///The default way to create a security connection from an access token
  def create(token: String): Future[SecurityConnection] = {
    if (FakeSecurity.isFakeAccessToken(token))
      FakeSecurity.createInMemoryFromFakeAccessToken(token, false) //Caching off because no speedup by caching the in-memory stuff!
    else
      Dataporten.createSecurityConnection(token, true)
  }

  ///The default way to create a security connection from a Htpp request (containing a bearer token)

  def create[T](request: Request[T]): Future[Either[MusitError, SecurityConnection]] = {
    request.getBearerToken match {
      case Some(token) => Security.create(token).toMusitFuture
      case None => MusitFuture.fromError(MusitError(401, "No token in request"))
    }
  }

  //internal stuff, move to another object?
  def createSecurityConnectionFromInfoProvider(infoProvider: ConnectionInfoProvider, useCache: Boolean): Future[SecurityConnection] = {
    val _infoProvider = if (useCache) new CachedConnectionInfoProvider(infoProvider) else infoProvider

    val userInfoF = _infoProvider.getUserInfo
    val userGroupIdsF = _infoProvider.getUserGroupIds

    for {
      userInfo <- userInfoF
      userGroupIds <- userGroupIdsF
    } yield new SecurityConnectionImp(_infoProvider, userInfo, userGroupIds)
  }
}