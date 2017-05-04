package controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest

import scala.concurrent.Future

package object storage {

  def invaludUuidResponse(arg: String): Future[Result] = Future.successful {
    BadRequest(Json.obj("message" -> s"Invalid UUID $arg"))
  }

}
