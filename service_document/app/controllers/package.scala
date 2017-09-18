import java.net.URLEncoder.encode

import no.uio.musit.MusitResults._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}

package object controllers {

  def respond[A](res: MusitResult[A])(success: A => Result): Result = {
    res match {
      case MusitSuccess(s)        => success(s)
      case MusitNotFound(msg)     => NotFound(Json.obj("msg" -> msg))
      case MusitGeneralError(msg) => BadRequest(Json.obj("msg" -> msg))
      case err: MusitError        => InternalServerError(Json.obj("msg" -> err.message))
    }
  }

  lazy val ContentDisposition = (filename: String) =>
    s"""attachment; filename="$filename"; filename*=UTF-8''""" +
      encode(filename, "UTF-8").replace("+", "%20")

}
