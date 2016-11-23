/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package controllers

import java.util.UUID
import java.net.URLEncoder.encode

import akka.stream.scaladsl.{Source, StreamConverters}
import com.google.inject.{Inject, Singleton}
import models.BarcodeFormats.{BarcodeFormat, DataMatrix, QrCode}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.Json
import services.{DataMatrixGenerator, Generator, QrGenerator}

import scala.util.{Failure, Success, Try}

@Singleton
class BarcodeController @Inject()(
  val authService: Authenticator
) extends MusitController {

  val logger = Logger(classOf[BarcodeController])

  def contentDisposition(name: String) = {
    CONTENT_DISPOSITION -> (s"""attachment; filename="$name.png"; filename*=UTF-8''""" +
        encode(name, "UTF-8").replace("+", "%20")
      )
  }

  def generate(
    uuid: String,
    width: Option[Int],
    height: Option[Int],
    format: Int = QrCode.code
  ) = MusitSecureAction() { implicit request =>
    Try(UUID.fromString(uuid)) match {
      case Success(id) =>
        BarcodeFormat.fromInt(format).flatMap { bf =>
          Generator.generatorFor(bf).map { generator =>
            generator.write(id, width, height).map { code =>
              Ok.chunked(Source(code.toList))
                .withHeaders(contentDisposition(uuid))
            }.getOrElse {
              InternalServerError(
                "message" -> s"An error occurred when trying to generate code for $uuid"
              )
            }
          }
        }.getOrElse {
          BadRequest(Json.obj(
            "message" -> (s"The argument format must be one " +
              s"of ${QrCode.code} (QrCode) or ${DataMatrix.code} (DataMatrix).")
          ))
        }

      case Failure(ex) =>
        val msg = s"The uuid argument must be a valid UUID [$uuid]"
        logger.warn(msg)
        BadRequest(Json.obj("message" -> msg))
    }
  }

}
