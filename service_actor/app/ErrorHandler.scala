import no.uio.musit.microservices.common.domain.MusitError
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Logger.warn(s"ErrorHandler - Client error ($statusCode): $message")
    Future.successful(
      Status(statusCode)(Json.toJson(MusitError(statusCode, message)))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("ErrorHandler - Server error", exception)
    Future.successful(
      InternalServerError(Json.toJson(MusitError(500, exception.getMessage)))
    )
  }
}