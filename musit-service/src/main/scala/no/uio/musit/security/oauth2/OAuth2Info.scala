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

package no.uio.musit.security.oauth2

import no.uio.musit.security.BearerToken
import play.api.libs.json.{Reads, __}
import play.api.libs.functional.syntax._

case class OAuth2Info(
    accessToken: BearerToken,
    tokenType: Option[String] = None,
    expiresIn: Option[Long] = None,
    refreshToken: Option[String] = None,
    params: Option[Map[String, String]] = None
)

object OAuth2Info extends OAuth2Constants {

  implicit val reads: Reads[OAuth2Info] = (
    (__ \ AccessToken).read[BearerToken] and
      (__ \ TokenType).readNullable[String] and
      (__ \ ExpiresIn).readNullable[Long] and
      (__ \ RefreshToken).readNullable[String]
  )(
    (accessToken, tokenType, expiresIn, refreshToken) =>
      OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)
  )

}
