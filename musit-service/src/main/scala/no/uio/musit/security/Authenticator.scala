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

import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.security.oauth2.OAuth2Info
import play.api.mvc.Request

import scala.concurrent.Future

/**
 * Definition of contract for how an authentication service should be
 * implemented. The AuthService is used to verify the authenticity of incoming
 * requests. Any of these services may or may not return
 */
trait Authenticator {

  /**
   * Starts the OAuth2 authentication process.
   *
   * @param req The current request.
   * @tparam A The type of the request body.
   * @return A MusitResult with the OAuth2Info from the authentication provider
   */
  def authenticate[A]()(implicit req: Request[A]): Future[MusitResult[OAuth2Info]]

  /**
   * Method for retrieving the UserInfo from the AuthService.
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  def userInfo(token: BearerToken): Future[MusitResult[UserInfo]]

  /**
   * Method for retrieving the users GroupInfo from the AuthService based
   * on the UserInfo found.
   *
   * @param userInfo the UserInfo found by calling the userInfo method above.
   * @return Will eventually return a Seq of GroupInfo
   */
  def groups(userInfo: UserInfo): Future[MusitResult[Seq[GroupInfo]]]

}
