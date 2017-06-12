package controllers.web

import com.google.inject.{Inject, Singleton}
import no.uio.musit.security.crypto.MusitCrypto
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import controllers.web

@Singleton
class Init @Inject()(
    val crypto: MusitCrypto
) extends Controller {

  val logger = Logger(classOf[Init])

  def init = Action(parse.urlFormEncoded) { implicit request =>
    request.body
      .get("_at")
      .flatMap { tokSeq =>
        tokSeq.headOption.map { tokStr =>
          logger.trace(s"Plain text token: $tokStr")

          val token = crypto.encryptAES(tokStr)

          logger.trace(s"Encrypted token: $token")

          val url = web.routes.MuseumController.listMuseums().url

          logger.debug(s"Redirecting to: $url")

          Redirect(
            url = url,
            queryString = Map("_at" -> Seq(token))
          )
        }
      }
      .getOrElse {
        Unauthorized(Json.obj("message" -> "Access denied."))
      }
  }

}
