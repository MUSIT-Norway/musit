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

import com.google.inject.{Inject, Singleton}
import models.BarcodeFormats.BarcodeFormat
import models.LabelData
import models.TemplateConfigs.{Avery5160, Herma9560, TemplateConfig}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Action

import scala.util.Try

@Singleton
class LabelController @Inject() (
    val authService: Authenticator
) extends MusitController {

  val logger = Logger(classOf[LabelController])

  def preview(
    templateId: Int,
    format: Int,
    name: String,
    uuid: String
  ) = Action { implicit request =>
    Try(UUID.fromString(uuid)).toOption.map { id =>
      BarcodeFormat.fromInt(format).map { bf =>
        TemplateConfig.fromInt(templateId).map {
          // TODO: For now this is OK...but eventually we'll need to accommodate
          // quite a few label templates. And a huuuuge pattern match is going
          // to feel quite messy.
          case Avery5160 => views.html.avery5160(id, name, bf, isPreview = true)
          case Herma9560 => views.html.herma9650(id, name, bf, isPreview = true)

        }.map(view => Ok(view)).getOrElse {
          BadRequest(views.html.error(s"Template Id $templateId is not valid"))
        }
      }.getOrElse {
        BadRequest(views.html.error("An error occurred trying to render the label"))
      }
    }.getOrElse {
      BadRequest(views.html.error(s"The argument $uuid is not a valid UUID"))
    }
  }

  def render(templateId: Int, format: Int) = Action(parse.json) { implicit request =>
    request.body.validate[Seq[LabelData]] match {
      case JsSuccess(data, _) =>
        BarcodeFormat.fromInt(format).map { bf =>
          TemplateConfig.fromInt(templateId).map {
            // TODO: For now this is OK...but eventually we'll need to accommodate
            // quite a few label templates. And a huuuuge pattern match is going
            // to feel quite messy.
            case Avery5160 => ??? //views.html.avery5160(id, name, bf)
            case Herma9560 => ??? // views.html.herma9650(id, name, bf)
          }

        }
        ???

      case err: JsError =>
        BadRequest(JsError.toJson(err))
    }
  }

}
