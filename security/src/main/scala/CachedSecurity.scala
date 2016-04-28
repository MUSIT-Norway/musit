import no.uio.musit.microservices.common.extensions.MusitCache
import no.uio.musit.security.{ConnectionInfoProvider, GroupInfo, UserInfo, UserInfoProvider}

import scala.concurrent.Future
import play.api.cache.Cache

import scala.concurrent.duration._
import play.api.Play.current
import play.cache.Cache

import scala.concurrent.ExecutionContext.Implicits.global
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

/**
  * Created by jstabel on 4/27/16.
  */


trait SecurityCache {
  ///provider maps access token to userId
  def accessTokenToUserId(accessToken: String, provider: String=>Future[String]) : Future[String]

  ///Primarily for testing
  def cachedAccessTokenToUserId(accessToken: String): Option[String]

  ///provider maps userId to UserInfo
  def userIdToUserInfo(userId: String, provider: String=>Future[UserInfo]) : Future[UserInfo]
  ///provider maps userId to groupIds
  def userIdToGroupIds(userId: String, provider: String => Future[Seq[String]]): Future[Seq[String]]

  def cache: play.api.cache.Cache.type
  //def getAs[A](key: String) = MusitCache.getAs[A](key)
}



class SecurityCacheImp extends SecurityCache{
  val securityPrefix = "Security"
  val defExpiry = 1 hour
  def accessTokenToUserId(accessToken: String, provider: String=>Future[String]) : Future[String] = {
    MusitCache.getOrElseFuture(s"$securityPrefix.AccessToken.$accessToken", defExpiry)(provider(accessToken))
  }

  def cachedAccessTokenToUserId(accessToken: String): Option[String] = MusitCache.cache.getAs[String](s"$securityPrefix.AccessToken.$accessToken")


  ///provider maps userId to UserInfo
  def userIdToUserInfo(userId: String, provider: String=>Future[UserInfo]) : Future[UserInfo] = {
    MusitCache.getOrElseFuture(s"$securityPrefix.UserIdToUserInfo.$userId", defExpiry)(provider(userId))
  }

  ///provider maps userId to groupIds
  def userIdToGroupIds(userId: String, provider: String => Future[Seq[String]]): Future[Seq[String]]= {
    MusitCache.getOrElseFuture(s"$securityPrefix.UserIdToGroupIds.$userId", defExpiry)(provider(userId))
  }

  def cache = MusitCache.cache
}


class CachedConnectionInfoProvider(infoProviderToCache: ConnectionInfoProvider) extends ConnectionInfoProvider {
  val securityPrefix = "Security"
  val defExpiry = 1 hour

  def accessToken: String = infoProviderToCache.accessToken

  def getAndMaybeCacheUserId: Future[String] = {
    val accesstokenToUserIdkey = s"$securityPrefix.AccessTokenToUserId.$accessToken"
    MusitCache.cache.getAs[String](accesstokenToUserIdkey) match {
      case Some(userId) => Future(userId)
      case None =>
        val userInfoF = infoProviderToCache.getUserInfo
        val idF = userInfoF.map(_.id)
        MusitCache.setFuture(accesstokenToUserIdkey, idF)
        idF
    }
  }

  def userIdToUserInfo(userId: String, provider: String => Future[UserInfo]): Future[UserInfo] = {
    MusitCache.getOrElseFuture(s"$securityPrefix.UserIdToUserInfo.$userId", defExpiry)(provider(userId))
  }


  def getUserInfo: Future[UserInfo] = {
    for {
      userId <- getAndMaybeCacheUserId
      userInfo <- userIdToUserInfo(userId, { _ => infoProviderToCache.getUserInfo })
    } yield userInfo
  }

  def userIdToGroups(userId: String, provider: String => Future[Seq[GroupInfo]]): Future[Seq[GroupInfo]] = {
    MusitCache.getOrElseFuture(s"$securityPrefix.UserIdToUserGroups.$userId", defExpiry)(provider(userId))
  }


  def getUserGroups: Future[Seq[GroupInfo]] = {
    for {
      userId <- getAndMaybeCacheUserId
      userGroups <- userIdToGroups(userId, { _ => infoProviderToCache.getUserGroups })
    } yield userGroups
  }

  def userIdToGroupIds(userId: String, provider: String => Future[Seq[String]]): Future[Seq[String]] = {
    MusitCache.getOrElseFuture(s"$securityPrefix.UserIdToUserGroupIds.$userId", defExpiry)(provider(userId))
  }

  override def getUserGroupIds: Future[Seq[String]] = {
    for {
      userId <- getAndMaybeCacheUserId
      userGroupsId <- userIdToGroupIds(userId, { _ => infoProviderToCache.getUserGroupIds })
    } yield userGroupsId
  }

}



