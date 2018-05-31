package controllers

import no.uio.musit.MusitResults._
import no.uio.musit.functional.FutureMusitResult
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

//TODO These are temporary helpers made in a hurry, ought to be done in a more proper way.
// And doesn't really belong in the controller-package!

object MusitResultUtils {

  def optionToMusitResult[T](opt: Option[T], errorIfNone: MusitError): MusitResult[T] = {
    opt match {
      case Some(t) => MusitSuccess(t)
      case None    => errorIfNone
    }
  }

  def defaultErrorTranslator(err: MusitError): Result = {
    err match {
      case err: MusitValidationError => badRequestErr(err)

      case err: MusitNotAuthenticated =>
        musitNotAuthenticatedErr(err)

      case err: MusitNotAuthorized =>
        musitNotAuthorized(err)

      case err: MusitError => internalErr(err)
    }
  }

  def musitResultToPlayResult[T](musitResult: MusitResult[T])(
      implicit w: Writes[T]
  ): Result = {
    musitResult match {
      case MusitSuccess(tt) => Results.Ok(Json.toJson(tt))
      case err: MusitError  => defaultErrorTranslator(err)
    }
  }

  def musitResultUnitToPlayResult(musitResult: MusitResult[Unit]): Result = {
    musitResult match {
      case MusitSuccess(_) => Results.NoContent
      case err: MusitError => defaultErrorTranslator(err)
    }
  }

  def musitResultSeqToPlayResult[T](musitResult: MusitResult[Seq[T]])(
      implicit w: Writes[T]
  ): Result = {
    musitResult match {
      case MusitSuccess(tt) => listAsPlayResult(tt)
      case err: MusitError  => defaultErrorTranslator(err)
    }
  }

  def futureMusitResultSeqToPlayResult[T](
      futMusitResult: FutureMusitResult[Seq[T]]
  )(
      implicit w: Writes[T],
      ec: ExecutionContext
  ): Future[Result] = {
    futMusitResult.value.map(musitResultSeqToPlayResult(_)(w))
  }

  /** Results in a 204 (NO_Content) if success */
  def futureMusitResultUnitToPlayResult(
      futMusitResult: FutureMusitResult[Unit]
  )(
      implicit
      ec: ExecutionContext
  ): Future[Result] = {
    futMusitResult.value.map(musitResultUnitToPlayResult(_))
  }

  def futureMusitResultToPlayResult[T](
      futMusitResult: FutureMusitResult[T]
  )(
      implicit w: Writes[T],
      ec: ExecutionContext
  ): Future[Result] = {
    futMusitResult.value.map(musitResultToPlayResult(_)(w))
  }

  implicit class MusitResultHelpers[T](val musitResult: MusitResult[T]) {

    def flatMapToFutureResult(
        f: T => Future[Result],
        errorTranslator: MusitError => Result = defaultErrorTranslator
    ): Future[Result] = {
      musitResult match {
        case MusitSuccess(t) => f(t)

        case err: MusitError => Future.successful(errorTranslator(err))
      }
    }
  }
  implicit class MusitErrorHelpers[T](val musitError: MusitError) {

    def toFuturePlayResult: Future[Result] =
      Future.successful(defaultErrorTranslator(musitError))
  }

}
