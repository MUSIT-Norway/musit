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

package controllers.rest

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.{Authenticator, BearerToken}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

/**
 * This controller will expose login and logout functionality for the MUSIT
 * system. It will handle the interaction with Dataporten to perform the OAuth2
 * authentication flow.
 */
class AuthenticationController @Inject() (
    implicit
    val authService: Authenticator
) extends MusitController {

  val logger = Logger(classOf[AuthenticationController])

  /**
   * Handles OAuth2 authentication flow against the configured
   * Authenticator service.
   */
  def authenticate = Action.async { implicit request =>
    authService.authenticate().map {
      case Left(res) => res
      case Right(userSession) =>
        logger.debug(s"Initialized new UserSesssion with id ${userSession.uuid}")
        Redirect("/").withHeaders(userSession.uuid.asBearerToken.asHeader)
    }
  }

  /**
   * Marks the UserSession associated with the bearer token in the request as
   * logged out.
   */
  def logout = MusitSecureAction().async { implicit request =>
    authService.invalidate(request.token).map {
      case MusitSuccess(()) => Ok
      case err: MusitError => InternalServerError(Json.obj("message" -> err.message))
    }
  }

}
