package controllers.conservation

import controllers._
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.functional.FutureMusitResult
import play.api.mvc.Result

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
      case err: MusitError           => internalErr(err)
    }
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
}
