package no.uio.musit.repositories

import no.uio.musit.MusitResults.{MusitDbError, MusitResult}
import play.api.Logger

import scala.util.control.NonFatal

trait DbErrorHandlers {

  private val logger = Logger(classOf[DbErrorHandlers])

  def nonFatal[A](msg: String): PartialFunction[Throwable, MusitResult[A]] = {
    case NonFatal(ex) =>
      logger.error(msg, ex)
      MusitDbError(msg, Option(ex))
  }

}
