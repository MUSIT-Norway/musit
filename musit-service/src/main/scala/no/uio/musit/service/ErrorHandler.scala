package no.uio.musit.service

import com.google.inject.Singleton
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

// $COVERAGE-OFF$
@Singleton
class ErrorHandler extends HttpErrorHandler {

  val logger = Logger(classOf[ErrorHandler])

  def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] = {
    logger.warn(s"ErrorHandler - Client error ($statusCode): $message")
    Future.successful(
      Status(statusCode)(Json.obj("status" -> statusCode, "message" -> message))
    )
  }

  def onServerError(
      request: RequestHeader,
      exception: Throwable
  ): Future[Result] = {
    logger.error("ErrorHandler - Server error", exception)
    Future.successful(
      InternalServerError(Json.obj("status" -> 500, "message" -> exception.getMessage))
    )
  }
}
// $COVERAGE-ON$
