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

package no.uio.musit.security.fake

import no.uio.musit.MusitResults._
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security._
import no.uio.musit.security.fake.FakeAuthenticator.FakeUserDetails
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Request

import scala.concurrent.Future
import scala.io.Source

class FakeAuthenticator extends Authenticator {

  private val fakeFile = "/fake_security.json"

  val config = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream(fakeFile))
    .getLines()
    .mkString
  )

  private case class FakeGroup(
    id: GroupId,
    name: String,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String],
    collections: Seq[CollectionUUID]
  )

  private implicit val formatFakeGroup = Json.format[FakeGroup]

  private lazy val allCols = (config \ "museumCollections").as[Seq[MuseumCollection]]

  private lazy val collectionsMap = allCols.map(c => (c.uuid, c)).toMap

  private lazy val allGroups = (config \ "groups").as[Seq[FakeGroup]].map { fg =>
    GroupInfo(
      id = fg.id,
      name = fg.name,
      permission = fg.permission,
      museumId = fg.museumId,
      description = fg.description,
      collections = fg.collections.map(cid => collectionsMap(cid))
    )
  }

  private lazy val fakeUsers: Map[BearerToken, FakeUserDetails] = {
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
   * Method for retrieving the users GroupInfo from the AuthService based
   * on the UserInfo found.
   *
   * @param userInfo the UserInfo found by calling the userInfo method above.
   * @return Will eventually return a Seq of GroupInfo
   */
  override def groups(userInfo: UserInfo): Future[MusitResult[Seq[GroupInfo]]] =
    Future.successful {
      MusitSuccess(
        fakeUsers.find(_._2.info.id == userInfo.id)
          .map(_._2.groups)
          .getOrElse(Seq.empty)
      )
    }

  override def authenticate[A]()(implicit req: Request[A]) = {
    Future.successful(
      MusitGeneralError("Authenticate method not implemented for fake security.")
    )
  }
}

object FakeAuthenticator {

  // Must match the fake_security.json file!
  val fakeAccessTokenPrefix = "fake-token-zab-xy-"

  case class FakeUserDetails(info: UserInfo, groups: Seq[GroupInfo])

}