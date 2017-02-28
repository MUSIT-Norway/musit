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

import no.uio.musit.models.ActorId
import no.uio.musit.security.oauth2.OAuth2Info
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime

case class UserSession(
    uuid: SessionUUID,
    oauthToken: Option[BearerToken] = None,
    userId: Option[ActorId] = None,
    loginTime: Option[DateTime] = None,
    lastActive: Option[DateTime] = None,
    isLoggedIn: Boolean = false,
    tokenExpiry: Option[Long] = None,
    client: Option[String] = None
) {

  def touch(timeoutMillis: Long): UserSession = {
    val now = dateTimeNow
    copy(
      lastActive = Option(now),
      tokenExpiry = Option(now.plus(timeoutMillis).getMillis)
    )
  }

  def activate(
    oauthInfo: OAuth2Info,
    userInfo: UserInfo,
    timeoutMillis: Long
  ): UserSession = {
    val now = dateTimeNow
    this.copy(
      oauthToken = Option(oauthInfo.accessToken),
      userId = Option(userInfo.id),
      loginTime = Option(now),
      lastActive = Option(now),
      isLoggedIn = true,
      tokenExpiry = Option(now.plus(timeoutMillis).getMillis)
    )
  }

}

object UserSession {

  def prepare(client: Option[String]): UserSession =
    UserSession(uuid = SessionUUID.generate(), client = client.map(_.toLowerCase))

}
