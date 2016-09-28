package no.uio.musit.security

import no.uio.musit.microservices.common.utils.ErrorHelper
import no.uio.musit.security.SecurityGroups.Permission
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

/**
 * Created by jarle on 26.09.16.
 */

class AuthRequest[A](val user: AuthenticatedUser, request: Request[A]) extends WrappedRequest[A](request)

class MusitSecureAction(val permissions: Permission*) extends ActionBuilder[AuthRequest] with ActionRefiner[Request, AuthRequest] {

  def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = {

    Security.create(request).map {
      case Right(authUser) =>
        if (authUser.hasAllPermissions(permissions.toSeq)) {
          Right(new AuthRequest(authUser, request))
        } else {
          Left(ErrorHelper.forbidden(
            "",
            s"user ${authUser.userName} doesn't have all of the permissions: $permissions"
          ).toPlayResult) //TODO: Log?

        }
      case Left(e) => Left(e.toPlayResult)
    }
  }
}

object MusitSecureAction {
  def apply(permissions: Permission*) = new MusitSecureAction(permissions: _*)
  def apply(museumId: Int, permissions: Permission*) = new MusitSecureAction(permissions: _*)
}