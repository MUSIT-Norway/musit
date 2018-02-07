package no.uio.musit.repositories

import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitValidationError}
import no.uio.musit.models.MusitSlickClientError
import play.api.Logger

import scala.util.control.NonFatal

trait DbErrorHandlers {

  private val logger = Logger(classOf[DbErrorHandlers])

  def nonFatal[A](msg: String): PartialFunction[Throwable, MusitResult[A]] = {
    case ex: MusitSlickClientError => {
      logger.error(msg, ex)
      //println(ex)
      MusitValidationError(msg, Option(ex))
    }
    case NonFatal(ex) =>
      logger.error(msg, ex)
      //println(ex)
      MusitDbError(msg + s" ex: ${ex.getLocalizedMessage()}", Option(ex))
  }

}
