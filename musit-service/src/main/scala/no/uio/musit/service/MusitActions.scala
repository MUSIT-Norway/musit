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

import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.MuseumId
import no.uio.musit.models.Museums._
import no.uio.musit.security.Permissions.{ElevatedPermission, MusitAdmin, Permission}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.{AuthenticatedUser, Authenticator, BearerToken, UserInfo}
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

  type MusitActionResult[T] = Either[Result, MusitRequest[T]]
  type MusitActionResultF[T] = Future[MusitActionResult[T]]

  type AuthFunc[T] = (BearerToken, UserInfo, AuthenticatedUser, Option[Museum]) => MusitActionResult[T] // scalastyle:ignore

  /**
   * The base representation of a MUSIT specific request.
   */
  abstract class BaseMusitAction extends ActionBuilder[MusitRequest]
      with ActionRefiner[Request, MusitRequest] {

    override def refine[T](request: Request[T]): MusitActionResultF[T]

  }

  abstract class BaseSecureAction extends BaseMusitAction {

    protected def auth[T](
      request: Request[T],
      museumId: Option[MuseumId],
      maybeToken: Option[BearerToken]
    )(authorize: AuthFunc[T]): MusitActionResultF[T] = {
      val museum = museumId.flatMap(Museum.fromMuseumId)
      maybeToken.map { token =>
        val res = for {
          session <- MusitResultT(authService.touch(token))
          userInfo <- MusitResultT(authService.userInfo(token))
          groups <- MusitResultT(authService.groups(userInfo))
        } yield {
          logger.debug(s"Got Groups\n${groups.map(_.name).mkString(", ")}")
          // TODO: Update AuthenticatedUser with UserSession information.
          val authUser = AuthenticatedUser(session, userInfo, groups)

          authorize(token, userInfo, authUser, museum)
        }

        res.value.map(_.getOrElse(Left(Unauthorized)))

      }.getOrElse {
        Future.successful(Left(Unauthorized))
      }
    }

  }

  /**
   * A custom Action that checks if the user is authenticated. If the request
   * contains a valid bearer token, the request is enriched with an
   * {{{AuthenticatedUser}}}. If the incoming request can't be authenticated
   * a {{{Result}}} with HTTP Forbidden is returned.
   *
   * @param museumId    An Option with the MuseumId for which the request wants info
   * @param permissions Varargs of Permission restrict who is authorized.
   */
  case class MusitSecureAction(
      museumId: Option[MuseumId],
      permissions: Permission*
  ) extends BaseSecureAction {
    override def refine[T](request: Request[T]): MusitActionResultF[T] = {
      val maybeToken = BearerToken.fromRequestHeader(request)
      auth(request, museumId, maybeToken) { (token, userInfo, authUser, museum) =>
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

trait MusitAdminActions extends MusitActions {

  private val logger = Logger(classOf[MusitAdminActions])

  /**
   * Crypto implementation to handle special cases for parsing the access token.
   */
  val crypto: MusitCrypto

  /**
   * Play Action that should be used for endpoints that require admin level
   * authorization. The lowest allowable permission is {{{MusitAdmin}}}.
   *
   * @param museumId    The MuseumId to access.
   * @param permissions The permissions required to access the endpoint.
   */
  case class MusitAdminAction(
      museumId: Option[MuseumId],
      permissions: ElevatedPermission*
  ) extends BaseSecureAction {

    override def refine[T](request: Request[T]): MusitActionResultF[T] = {
      val maybeToken = BearerToken.fromRequestHeader(request).orElse(
        request.getQueryString("_at").map { qs =>
          val decrypted = crypto.decryptAES(qs)
          BearerToken(decrypted)
        }
      )

      auth(request, museumId, maybeToken) { (token, userInfo, authUser, museum) =>
        authUser.authorizeAdmin(museum, permissions).map { empty =>
          Right(MusitRequest(authUser, token, museum, request))
        }.getOrElse {
          logger.debug(s"Action is unauthorized for ${userInfo.id}")
          Left(Forbidden)
        }
      }
    }
  }

  object MusitAdminAction {
    def apply(): MusitAdminAction = MusitAdminAction(permissions = MusitAdmin)

    def apply(mid: MuseumId): MusitAdminAction = MusitAdminAction(Some(mid))

    def apply(permissions: ElevatedPermission*): MusitAdminAction =
      MusitAdminAction(None, permissions: _*)

    def apply(mid: MuseumId, permissions: ElevatedPermission*): MusitAdminAction =
      MusitAdminAction(Some(mid), permissions: _*)
  }

}