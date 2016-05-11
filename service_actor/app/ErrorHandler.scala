import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsError, Json}
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Logger.warn(s"ErrorHandler - Client error ($statusCode): $message")
    Future.successful(
      Status(statusCode)(Json.obj("status" -> statusCode, "message" -> message))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("ErrorHandler - Server error", exception)
    Future.successful(
      InternalServerError(Json.obj("status" -> 500, "message" -> exception.getMessage))
    )
  }
}