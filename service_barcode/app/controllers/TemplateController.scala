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
import models.{FieldData, LabelData, TemplateConfigs}
import models.TemplateConfigs._
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.json._

import scala.util.Try

@Singleton
class TemplateController @Inject()(
    val authService: Authenticator
) extends MusitController {

  val logger = Logger(classOf[TemplateController])

  def preview(
      templateId: Int,
      codeFormat: Int,
      name: String,
      uuid: String
  ) = MusitSecureAction() { implicit request =>
    Try(UUID.fromString(uuid)).toOption.map { id =>
      val labelData = Seq(LabelData(uuid, Seq(FieldData(Some("name"), name))))
      BarcodeFormat
        .fromInt(codeFormat)
        .map { bf =>
          TemplateConfig
            .fromInt(templateId)
            .map {
              // TODO: For now this is OK...but eventually we'll need to accommodate
              // quite a few label templates. And a huuuuge pattern match is going
              // to feel quite messy.
              case Label1 =>
                views.html.label1(labelData, bf, Label1, isPreview = true)

              case Label2 =>
                views.html.label2(labelData, bf, Label2, isPreview = true)

              case Label3 =>
                views.html.label3(labelData, bf, Label3, isPreview = true)

              case Label4 =>
                views.html.label4(labelData, bf, Label4, isPreview = true)

              case Label5 =>
                views.html.label5(labelData, bf, Label5, isPreview = true)

              case LabelPrinter =>
                views.html.label_printer(labelData, bf, LabelPrinter, isPreview = true)

            }
            .map { view =>
              Ok(view)
            }
            .getOrElse {
              BadRequest(views.html.error(s"Template Id $templateId is not valid"))
            }
        }
        .getOrElse {
          BadRequest(views.html.error(s"Unsupported barcode format $codeFormat"))
        }
    }.getOrElse {
      BadRequest(views.html.error(s"The argument $uuid is not a valid UUID"))
    }
  }

  def render(
      templateId: Int,
      codeFormat: Int
  ) = MusitSecureAction()(parse.json) { implicit request =>
    request.body.validate[Seq[LabelData]] match {
      case JsSuccess(data, _) =>
        BarcodeFormat
          .fromInt(codeFormat)
          .map { bf =>
            TemplateConfig
              .fromInt(templateId)
              .map {
                // TODO: For now this is OK...but eventually we'll need to accommodate
                // quite a few label templates. And a huuuuge pattern match is going
                // to feel quite messy.
                case Label1 =>
                  views.html.label1(data, bf, Label1)

                case Label2 =>
                  views.html.label2(data, bf, Label2)

                case Label3 =>
                  views.html.label3(data, bf, Label3)

                case Label4 =>
                  views.html.label4(data, bf, Label4)

                case Label5 =>
                  views.html.label5(data, bf, Label5)

                case LabelPrinter =>
                  views.html.label_printer(data, bf, LabelPrinter)

              }
              .map { view =>
                Ok(view)
              }
              .getOrElse {
                BadRequest(views.html.error(s"Template Id $templateId is not valid"))
              }
          }
          .getOrElse {
            BadRequest(views.html.error(s"Unsupported barcode format $codeFormat"))
          }

      case err: JsError =>
        BadRequest(JsError.toJson(err))
    }
  }

  def listTemplates = MusitSecureAction() { implicit request =>
    val tjs = TemplateConfigs.AvailableConfigs.map { c =>
      Json.obj(
        "id"          -> c.templateId,
        "name"        -> c.name,
        "labelWidth"  -> c.labelWidth.underlying,
        "labelHeight" -> c.labelHeight.underlying,
        "colsPerPage" -> c.colsPerPage,
        "rowsPerPage" -> c.rowsPerPage
      )
    }
    Ok(JsArray(tjs))
  }

}
