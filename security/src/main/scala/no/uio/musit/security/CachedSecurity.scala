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

import no.uio.musit.microservices.common.extensions.MusitCache
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by jstabel on 4/27/16.
 */

///Only used for testing, for direct read access to some of the cached info, to check whether the cache contains want we belive it contains
trait SecurityCacheReader {
  def accessTokenToUserIdFromCache(accessToken: String): Option[String]

  def accessTokenToUserInfoFromCache(accessToken: String): Option[UserInfo]

  def accessTokenToGroups(accessToken: String): Option[Seq[GroupInfo]]
}

/*
* Here we cache the following mappings:
* accessToken => userId
* userId => UserInfo
* userId => Groups
*
* This way, the UserInfo and Groups caching doesn't get irrelevant when the same user connects with a new token.
 * However, it may perhaps make sense to not remember the cached items longer than the given connection (this may help debugging and when giving users new group permissions etc
  * (just log out and restart instead of us having to push a button to clear the/some cache). This would also simplify the code in this class a bit.
* *
* */
class CachedConnectionInfoProvider(infoProviderToCache: ConnectionInfoProvider) extends ConnectionInfoProvider with SecurityCacheReader {
  val securityPrefix = "Security"
  val defExpiry = 1 hour

  val accessToken: String = infoProviderToCache.accessToken

  val accessTokenToUserIdKey = s"$securityPrefix.AccessTokenToUserId.$accessToken"

  def userIdToUserInfoKey(userId: String) = s"$securityPrefix.UserIdToUserInfo.$userId"

  def userIdToGroupsKey(userId: String) = s"$securityPrefix.UserIdToUserGroups.$userId"

  def getAndMaybeCacheUserId: Future[String] = {
    MusitCache.getAs[String](accessTokenToUserIdKey) match {
      case Some(userId) => Future(userId)
      case None =>
        val userInfoF = infoProviderToCache.getUserInfo
        val idF = userInfoF.map(_.id)
        MusitCache.setFuture(accessTokenToUserIdKey, idF)
        idF
    }
  }

  def userIdToUserInfo(userId: String, provider: String => Future[UserInfo]): Future[UserInfo] = {
    MusitCache.getOrElseFuture(userIdToUserInfoKey(userId), defExpiry)(provider(userId))
  }

  def getUserIdFromCache = MusitCache.getAs[String](accessTokenToUserIdKey)

  def getUserInfo: Future[UserInfo] = {
    getUserIdFromCache match {
      case Some(userId) => userIdToUserInfo(userId, { _ => infoProviderToCache.getUserInfo })
      case None => {
        val userInfoF = infoProviderToCache.getUserInfo
        userInfoF.onSuccess {
          case userInfo =>
            MusitCache.set(userIdToUserInfoKey(userInfo.id), userInfo, defExpiry)
        }
        userInfoF
      }
    }
    /*  The below code is much simpler and would work, but would hit the external (typically Dataporten) server twice with a getUserInfo call, once to get the userId (via getUserInfo) and then again to get the userInfo.
        for {
          userId <- getAndMaybeCacheUserId
          userInfo <- userIdToUserInfo(userId, { _ => infoProviderToCache.getUserInfo })
        } yield userInfo
        */
  }

  def userIdToGroups(userId: String, provider: String => Future[Seq[GroupInfo]]): Future[Seq[GroupInfo]] = {
    MusitCache.getOrElseFuture(userIdToGroupsKey(userId), defExpiry)(provider(userId))
  }

  def getUserGroups: Future[Seq[GroupInfo]] = {
    for {
      userId <- getAndMaybeCacheUserId
      userGroups <- userIdToGroups(userId, { _ => infoProviderToCache.getUserGroups })
    } yield userGroups
  }

  ///Api for testing, implementing the SecurityCacheReader trait
  def accessTokenToUserIdFromCache(accessToken: String): Option[String] = MusitCache.getAs[String](accessTokenToUserIdKey)

  ///Api for testing, implementing the SecurityCacheReader trait
  def accessTokenToUserInfoFromCache(accessToken: String): Option[UserInfo] = {
    for {
      userId <- this.getUserIdFromCache
      userInfo <- MusitCache.getAs[UserInfo](userIdToUserInfoKey(userId))
    } yield userInfo
  }

  ///Api for testing, implementing the SecurityCacheReader trait
  def accessTokenToGroups(accessToken: String): Option[Seq[GroupInfo]] = {
    for {
      userId <- this.getUserIdFromCache
      groups <- MusitCache.getAs[Seq[GroupInfo]](userIdToGroupsKey(userId))
    } yield groups
  }
}

