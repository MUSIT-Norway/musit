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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by jstabel on 4/15/16.
  */

case class UserInfo(id: String, name: String)

case class GroupInfo(groupType: String, id: String, displayName: String , description: Option[String])

//type TokenToUserIdProvider =  (String) => String

/**Represents what a "connection" is expected to know of the current user
  *
   */
trait ConnectionInfoProvider {
  def getUserInfo : Future[UserInfo]
  def getUserGroups: Future[Seq[GroupInfo]]
  def getUserGroupIds: Future[Seq[String]] = getUserGroups.map(groupInfos => groupInfos.map(groupInfo => groupInfo.id))
}

trait GroupInfoProvider {
  def getGroupInfo(groupid: String) : Future[Option[GroupInfo]]
}

trait UserInfoProvider {
  def getUserInfo(userid: String) : Future[Option[UserInfo]]
  def getUserGroups(userid: String): Future[Option[Seq[String]]]
}
//TODO? def getGroupInfo(groupid: String) : Future[Option[GroupInfo]]


trait SecurityState {
  def userName: String
  def hasGroup(group: String): Boolean
  def hasAllGroups(groups: Seq[String]): Boolean
  def hasNoneOfGroups(groups: Seq[String]): Boolean
}


trait SecurityConnection {
  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T]
  def state: SecurityState
  def userName: String = state.userName
  def hasGroup(groupid: String) : Boolean = state.hasGroup(groupid)
  def hasAllGroups(groupIds: Seq[String]): Boolean = state.hasAllGroups(groupIds)
  def hasNoneOfGroups(groupIds: Seq[String]): Boolean = state.hasNoneOfGroups(groupIds)
}

class SecurityStateImp(_userName: String, userGroups: Seq[String]) extends SecurityState {
  import  no.uio.musit.microservices.common.extensions.SeqExtensions._

  override def userName: String = _userName

  def hasGroup(group: String) = userGroups.contains(group)
  def hasAllGroups(groups: Seq[String]) = userGroups.hasAllOf(groups)
  def hasNoneOfGroups(groups: Seq[String]) = userGroups.hasNoneOf(groups)

}


abstract class SecurityConnectionBaseImp(userName: String, userGroups: Seq[String]) extends SecurityConnection {
  val state = new SecurityStateImp(userName, userGroups)

  override def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => T): Try[T] = {
    if (state.hasAllGroups(requiredGroups) && state.hasNoneOfGroups(deniedGroups)) {
      Success(body)
    }
    else {
      //#OLD Future.failed(new Exception("Unauthorized"))
      val missingGroups = requiredGroups.filter((g => !(state.hasGroup(g))))
      val disallowedGroups = deniedGroups.filter(g => (state.hasGroup(g)))

      assert(missingGroups.length>0 || disallowedGroups.length>0)

      val missingGroupsText = Some(s"Missing groups: ${missingGroups.mkString(",")}.").filter(_=> !missingGroups.isEmpty)
      val shouldNotHaveGroupsText = Some(s"Having disallowed groups: ${disallowedGroups.mkString(",")}.").filter(_=> !disallowedGroups.isEmpty)

      val msg = s"Unauthorized! ${missingGroupsText.map(_ + " ").getOrElse("")}${shouldNotHaveGroupsText.getOrElse()}"

      Failure(new Exception(msg))
      }
    }
}


