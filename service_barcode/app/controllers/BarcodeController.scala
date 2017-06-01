package controllers

import java.net.URLEncoder.encode
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.BarcodeFormats.{BarcodeFormat, DataMatrix, QrCode}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import services.Generator

import scala.util.{Failure, Success, Try}

@Singleton
class BarcodeController @Inject()(
    val authService: Authenticator
) extends MusitController {

  val logger = Logger(classOf[BarcodeController])

  def contentDisposition(name: String) = {
    CONTENT_DISPOSITION -> (s"""inline; filename="$name.png"; filename*=UTF-8''""" +
      encode(name, "UTF-8").replace("+", "%20"))
  }

  def generate(
      uuid: String,
      format: Int = QrCode.code
  ) = Action {
    Try(UUID.fromString(uuid)) match {
      case Success(id) =>
        BarcodeFormat
          .fromInt(format)
          .flatMap { bf =>
            Generator.generatorFor(bf).map { generator =>
              generator
                .write(id)
                .map { code =>
                  Ok.chunked(code)
                    .withHeaders(
                      contentDisposition(uuid),
                      CONTENT_TYPE -> "image/png"
                    )
                }
                .getOrElse {
                  InternalServerError(
                    Json.obj(
                      "message" -> s"An error occurred when trying to generate code for $uuid"
                    )
                  )
                }
            }
          }
          .getOrElse {
            BadRequest(
              Json.obj(
                "message" -> (s"The argument format must be one " +
                  s"of ${QrCode.code} (QrCode) or ${DataMatrix.code} (DataMatrix).")
              )
            )
          }

      case Failure(ex) =>
        val msg = s"The uuid argument must be a valid UUID [$uuid]"
        logger.warn(msg)
        BadRequest(Json.obj("message" -> msg))
    }
  }

}
