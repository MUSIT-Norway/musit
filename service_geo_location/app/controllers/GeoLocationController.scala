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

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitHttpError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.GeoLocationService

import scala.util.control.NonFatal

class GeoLocationController @Inject()(
    val authService: Authenticator,
    val geoLocService: GeoLocationService
) extends MusitController {

  val logger = Logger(classOf[GeoLocationController])

  /**
   * Service for looking up addresses in the geo-location service provided by
   * kartverket.no
   */
  def searchExternal(
      search: Option[String]
  ) = MusitSecureAction().async { implicit request =>
    val expression = search.getOrElse("")
    geoLocService
      .searchGeoNorway(expression)
      .map {
        case MusitSuccess(locations) =>
          Ok(Json.toJson(locations))

        case MusitHttpError(status, msg) =>
          Status(status)

        case err: MusitError =>
          logger.error("InternalServerError: " + s"${err.message}")
          InternalServerError(Json.obj("message" -> err.message))
      }
      .recover {
        case NonFatal(ex) =>
          val msg = "An error occurred when searching for address"
          logger.error(msg, ex)
          InternalServerError(Json.obj("message" -> msg))
      }
  }

}
