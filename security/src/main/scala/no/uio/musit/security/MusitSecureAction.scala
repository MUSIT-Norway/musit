package no.uio.musit.security

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by jarle on 26.09.16.
  */


class AuthRequest[A](val user: AuthenticatedUser, request: Request[A]) extends WrappedRequest[A](request)

object MusitSecureAction extends ActionBuilder[AuthRequest] with ActionRefiner[Request, AuthRequest] {

  def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = {

     Security.create(request).map{
       case Right(authUser) => Right(new AuthRequest(authUser, request))
       case Left(e) => Left(e.toPlayResult)
     }
  }
  /*
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    Logger.info("Calling action")
    block(request)
  }*/
}