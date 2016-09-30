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
import no.uio.musit.security.SecurityGroups.Permission
import play.api.mvc.Request
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * Created by jstabel on 4/15/16.
 */

case class UserInfo(id: String, name: String, email: Option[String] = None)

case class GroupInfo(groupType: String, id: String, displayName: String, description: Option[String])

/**
 * Represents what a "connection" is expected to know of the current user
 *
 */
trait ConnectionInfoProvider {
  def getUserInfo: Future[UserInfo]

  def getUserGroups: Future[Seq[GroupInfo]]

  def accessToken: String
}

trait AuthenticatedUser {
  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T]

  def userName: String

  def userId: String

  def userEmail: Option[String]

  def hasGroup(groupid: String): Boolean

  def hasAllGroups(groupIds: Seq[String]): Boolean

  def hasNoneOfGroups(groupIds: Seq[String]): Boolean

  //:TODO Probably move museum to the constructor of this type instead of a parameter here.
  def hasAllPermissions(museum: Museum, permissions: Seq[Permission]): Boolean

  def groupIds: Seq[String]
  def groups: Seq[GroupInfo]
}

trait FakeAuthenticatedUser extends AuthenticatedUser {
  ///Accessing this is probably only relevant for testing/debugging
  def infoProvider: ConnectionInfoProvider
}

case class UserAndGroupInfo(userInfo: UserInfo, groups: Seq[GroupInfo])

class AuthenticatedUserImp(_infoProvider: ConnectionInfoProvider, userAndGroupInfo: UserAndGroupInfo) extends AuthenticatedUser {

  import no.uio.musit.microservices.common.extensions.SeqExtensions._

  val userInfo = userAndGroupInfo.userInfo
  val userGroups = userAndGroupInfo.groups
  val userGroupIds = userGroups.map(_.id)

  val allGroups = userGroupIds.map(SecurityGroups.fromGroupId).filterNot(_.isEmpty).map(_.get)

  val allPermissions = allGroups.flatMap(group => group.permissions)

  //Future: If we later move museum into the constructor, we can precalculate this instead of doing it on the fly
  private def permissionsRelativeToMuseum(museum: Museum) = {
    allGroups.flatMap(g => g.permissionsRelativeToMuseum(museum))
  }

  def userName: String = userInfo.name

  def userId: String = userInfo.id

  def userEmail: Option[String] = userInfo.email

  def hasAllPermissions(museum: Museum, permissions: Seq[Permission]): Boolean = {
    permissionsRelativeToMuseum(museum).hasAllOf(permissions)
  }

  def hasGroup(groupId: String) = userGroupIds.contains(groupId)

  def hasAllGroups(groupIds: Seq[String]) = userGroupIds.hasAllOf(groupIds)

  def hasNoneOfGroups(groupIds: Seq[String]) = userGroupIds.hasNoneOf(groupIds)

  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T] = {
    if (hasAllGroups(requiredGroups) && hasNoneOfGroups(deniedGroups)) {
      Success(body)
    } else {

      val missingGroups = requiredGroups.filter((g => !(hasGroup(g))))
      val disallowedGroups = deniedGroups.filter(g => (hasGroup(g)))

      assert(missingGroups.length > 0 || disallowedGroups.length > 0)

      val missingGroupsText = Some(s"Missing groups: ${missingGroups.mkString(",")}.").filter(_ => !missingGroups.isEmpty)
      val shouldNotHaveGroupsText = Some(s"Having disallowed groups: ${disallowedGroups.mkString(",")}.").filter(_ => !disallowedGroups.isEmpty)

      val msg = s"""Unauthorized! ${missingGroupsText.map(_ + " ").getOrElse("")}${shouldNotHaveGroupsText.getOrElse("")}"""

      Failure(new Exception(msg))
    }
  }

  def groups = userGroups
  def groupIds = userGroupIds
}

object Security {

  val noTokenInRequestMsg = "No token in request"

  ///The default way to create a security connection from an access token
  def create(token: String): Future[AuthenticatedUser] = {
    if (FakeSecurity.isFakeAccessToken(token)) {
      FakeSecurity.createInMemoryFromFakeAccessToken(token, false) //Caching off because no speedup by caching the in-memory stuff!
    } else {
      Dataporten.createAuthenticatedUser(token, true)
    }
  }

  ///The default way to create a security connection from a Htpp request (containing a bearer token)

  def create[T](request: Request[T]): Future[Either[MusitError, AuthenticatedUser]] = {
    request.getBearerToken match {
      case Some(token) => Security.create(token).toMusitFuture
      case None => MusitFuture.fromError(MusitError(401, noTokenInRequestMsg))
    }
  }
}

object SecurityUtils {
  def internalCreateAuthenticatedUserFromInfoProvider(infoProvider: ConnectionInfoProvider, useCache: Boolean,
    factory: (ConnectionInfoProvider, UserAndGroupInfo) => AuthenticatedUser): Future[AuthenticatedUser] = {
    val _infoProvider = if (useCache) new CachedConnectionInfoProvider(infoProvider) else infoProvider

    val userInfoF = _infoProvider.getUserInfo
    val userGroupsF = _infoProvider.getUserGroups

    for {
      userInfo <- userInfoF
      userGroups <- userGroupsF
    } yield factory(_infoProvider, UserAndGroupInfo(userInfo, userGroups))
  }

  def createAuthenticatedUserFromInfoProvider(infoProvider: ConnectionInfoProvider, useCache: Boolean): Future[AuthenticatedUser] = {
    def authUserFactory(infoProvider: ConnectionInfoProvider, userAndGroupInfo: UserAndGroupInfo) =
      new AuthenticatedUserImp(infoProvider, userAndGroupInfo)

    internalCreateAuthenticatedUserFromInfoProvider(infoProvider, useCache, authUserFactory)
  }
}

