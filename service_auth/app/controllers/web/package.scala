package controllers

import no.uio.musit.security.{AuthenticatedUser, EncryptedToken}
import play.api.mvc.Result

import scala.concurrent.Future
import play.api.mvc.Results._

package object web {

  private[web] def notFound(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Result = NotFound(views.html.error(user, encTok, msg))

  private[web] def notFoundF(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Future[Result] = Future.successful(notFound(user, encTok, msg))

  private[web] def badRequest(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Result = BadRequest(views.html.error(user, encTok, msg))

  private[web] def badRequestF(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Future[Result] = Future.successful(badRequest(user, encTok, msg))

  private[web] def serverErr(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Result = InternalServerError(views.html.error(user, encTok, msg))

  private[web] def serverErrF(
      user: AuthenticatedUser,
      encTok: EncryptedToken,
      msg: String
  ): Future[Result] = Future.successful(serverErr(user, encTok, msg))

}
