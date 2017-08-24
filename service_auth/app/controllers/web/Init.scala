package controllers.web

import com.google.inject.{Inject, Singleton}
import controllers.web
import no.uio.musit.security.crypto.MusitCrypto
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class Init @Inject()(
    cc: ControllerComponents,
    crypto: MusitCrypto
) extends AbstractController(cc) {

  val logger = Logger(classOf[Init])

  def init = Action(parse.formUrlEncoded) { implicit request =>
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
