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


/**
  * Created by jstabel on 4/15/16.
  */

case class UserInfo(id: String, name: String)

case class GroupInfo(groupType: String, id: String, displayName: String , description: Option[String])

//type TokenToUserIdProvider =  (String) => String

trait UserInfoProvider {
  def getUserInfo : Future[UserInfo]
  def getUserGroups: Future[Seq[GroupInfo]]
  def getUserGroupIds: Future[Seq[String]] = getUserGroups.map(groupInfos => groupInfos.map(groupInfo => groupInfo.id))
}

trait GroupInfoProvider {
  def getGroupInfo(groupid: String) : Future[GroupInfo]
}

trait CachedUserInfoProvider {
  def getUserInfo(userid: String) : Future[Option[UserInfo]]
  def getUserGroups(userid: String): Future[Option[Seq[String]]]
}
//TODO? def getGroupInfo(groupid: String) : Future[Option[GroupInfo]]


trait SecuritySupport {
  def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => Future[T]): Future[T]
  def userName: String
//  def context: SecurityContext
  def hasGroup(groupid: String) : Boolean
  def hasAllGroups(groups: Seq[String]): Boolean
  def hasNoneOfGroups(groups: Seq[String]): Boolean
  //val infoProvider: UserInfoProvider
}

class SecurityContext(userGroups: Seq[String]) {
  import  no.uio.musit.microservices.common.extensions.SeqExtensions._

  //val userGroups: Seq[String] = Seq.empty
  def hasGroup(group: String) = userGroups.contains(group)
  def hasAllGroups(groups: Seq[String]) = userGroups.hasAllOf(groups)
  def hasNoneOfGroups(groups: Seq[String]) = userGroups.hasNoneOf(groups)

}


abstract class SecuritySupportBaseImp(userGroups: Seq[String]) extends SecuritySupport {
  val ctx = new SecurityContext(userGroups)

  override def authorize[T](requiredGroups: Seq[String], deniedGroups: Seq[String] = Seq.empty)(body: => Future[T]): Future[T] = {
    if (ctx.hasAllGroups(requiredGroups) && ctx.hasNoneOfGroups(deniedGroups)) {
      body
    }
    else {
      Future.failed(new Exception("Unauthorized"))
    }
  }


  def hasGroup(groupid: String) : Boolean = ctx.hasGroup(groupid)
  def hasAllGroups(groups: Seq[String]): Boolean = ctx.hasAllGroups(groups)
  def hasNoneOfGroups(groups: Seq[String]): Boolean = ctx.hasNoneOfGroups(groups)
  //val infoProvider: UserInfoProvider
}


