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

package no.uio.musit.security

import no.uio.musit.models.{ActorId, GroupId}
import no.uio.musit.security.FakeAuthenticator.FakeUserDetails
import no.uio.musit.service.MusitResults._
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.Future
import scala.io.Source

class FakeAuthenticator extends Authenticator {

  private val fakeFile = "/fake_security.json"

  val config = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream(fakeFile))
    .getLines()
    .mkString
  )

  lazy val allGroups: Seq[GroupInfo] = (config \ "groups").as[Seq[GroupInfo]]

  lazy val fakeUsers: Map[BearerToken, FakeUserDetails] = {
    (config \ "users").as[JsArray].value.map { usrJs =>
      val token = BearerToken((usrJs \ "accessToken").as[String])
      val usrGrps = (usrJs \ "groups").as[Seq[GroupId]]
      val usrInfo = usrJs.as[UserInfo]
      val userGroups = allGroups.filter(g => usrGrps.contains(g.id))

      (token, FakeUserDetails(usrInfo, userGroups))
    }.toMap
  }

  /**
   * Method for retrieving the UserInfo from the FakeAuthService.
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  override def userInfo(token: BearerToken): Future[MusitResult[UserInfo]] = {
    Future.successful {
      fakeUsers.get(token).map { fud =>
        MusitSuccess(fud.info)
      }.getOrElse {
        MusitNotAuthenticated()
      }
    }
  }

  /**
   * Method for retrieving the users GroupInfo from the AuthService.
   *
   * @param userId the ActorId of the user to fetch groups for
   * @return a Future collection of GroupInfo
   */
  override def groups(
    userId: ActorId
  ): Future[Seq[GroupInfo]] = Future.successful {
    fakeUsers.find(_._2.info.id == userId).map(_._2.groups).getOrElse(Seq.empty)
  }

}

object FakeAuthenticator {

  // Must match the fake_security.json file!
  val fakeAccessTokenPrefix = "fake-token-zab-xy-"

  case class FakeUserDetails(info: UserInfo, groups: Seq[GroupInfo])

}