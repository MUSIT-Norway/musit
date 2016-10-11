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

class MusitSecureAction(val optMuseum: Option[Museum], val permissions: Permission*) extends ActionBuilder[AuthRequest]
    with ActionRefiner[Request, AuthRequest] {

  def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = {

    Security.create(request).map {
      case Right(authUser) =>
        if (permissions.isEmpty) {
          Right(new AuthRequest(authUser, request))
        } else {
          optMuseum match {
            case Some(museum) =>

              if (authUser.hasAllPermissions(museum, permissions.toSeq)) {
                Right(new AuthRequest(authUser, request))
              } else {
                Left(ErrorHelper.forbidden(
                  "",
                  s"user ${authUser.userName} doesn't have all of the permissions: $permissions"
                ).toPlayResult) //TODO: Log?
              }
            case None =>
              Left(ErrorHelper.forbidden(
                "",
                s"missing museum on the permission test"
              ).toPlayResult) //TODO: Log?
          }
        }
      case Left(e) => Left(e.toPlayResult)
    }
  }
}

object MusitSecureAction {
  def apply() /*(permissions: Permission*)*/ = new MusitSecureAction(None)

  def apply(museumId: Int, permissions: Permission*) = {
    val optMuseum = Museum.fromMuseumId(MuseumId(museumId))
    new MusitSecureAction(optMuseum, permissions: _*)
  }
}