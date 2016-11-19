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

package no.uio.musit.service

import no.uio.musit.models.MuseumId
import no.uio.musit.models.Museums._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security.{AuthenticatedUser, Authenticator, BearerToken}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

trait MusitActions {

  private val logger = Logger(classOf[MusitActions])

  def authService: Authenticator

  /**
   * Every request that is successfully authenticated against dataporten, will
   * be transformed into a MusitRequest. It contains information necessary for
   * calculating authorisation and filtering of data.
   *
   * @param user    The authenticated user.
   * @param token   A valid BearerToken
   * @param museum  An optional Museum derived from an incoming MuseumId
   * @param request The incoming request
   * @tparam A Body content type of the incoming request
   */
  case class MusitRequest[A](
    user: AuthenticatedUser,
    token: BearerToken,
    museum: Option[Museum],
    request: Request[A]
  ) extends WrappedRequest[A](request)

  type MusitActionResult[T] = Future[Either[Result, MusitRequest[T]]]

  /**
   * The base representation of a MUSIT specific request.
   */
  abstract class BaseMusitAction extends ActionBuilder[MusitRequest]
      with ActionRefiner[Request, MusitRequest] {

    override def refine[T](request: Request[T]): MusitActionResult[T]

  }

  /**
   * TODO: Need to add authorization permissions to restrict what operations
   * should be allowed on any given action. (Read, Write, Admin)
   *
   * A custom Action refiner that checks if the user is authenticated. If the
   * request contains a valid bearer token, the request is enriched with an
   * {{{AuthenticatedUser}}}. If the incoming request can't be authenticated
   * a {{{Result}}} with HTTP Forbidden is returned.
   *
   * @param museumId      An Option with the MuseumId for which the request wants info
   * @param permissions Varargs of Permission restrict who is authorized.
   */
  case class MusitSecureAction(
      museumId: Option[MuseumId],
      permissions: Permission*
  ) extends BaseMusitAction {
    override def refine[T](request: Request[T]): MusitActionResult[T] = {
      BearerToken.fromRequest(request).map { token =>
        for {
          usrRes <- authService.userInfo(token)
          grpsRes <- authService.groups(token)
        } yield {
          usrRes.flatMap { userInfo =>
            grpsRes.map { groups =>
              val authUser = AuthenticatedUser(userInfo, groups)
              val museum = museumId.flatMap(Museum.fromMuseumId)
              museum match {
                case Some(m) =>
                  authUser.authorize(m, permissions).map { empty =>
                    Right(MusitRequest(authUser, token, museum, request))
                  }.getOrElse {
                    logger.debug(s"Action is unauthorized for ${userInfo.id}")
                    Left(Forbidden)
                  }

                case None =>
                  if (museumId.isDefined) {
                    Left(BadRequest(Json.obj("message" -> s"Unknown museum $museumId")))
                  } else {
                    Right(MusitRequest(authUser, token, museum, request))
                  }
              }
            }
          }.getOrElse(Left(Unauthorized))
        }
      }.getOrElse {
        Future.successful(Left(Unauthorized))
      }
    }
  }

  object MusitSecureAction {

    def apply(): MusitSecureAction = MusitSecureAction(None)

    def apply(mid: MuseumId): MusitSecureAction = MusitSecureAction(Some(mid))

    def apply(permissions: Permission*): MusitSecureAction =
      MusitSecureAction(None, permissions: _*)

    def apply(mid: MuseumId, permissions: Permission*): MusitSecureAction =
      MusitSecureAction(Some(mid), permissions: _*)

  }

}