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
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import services.StatsService

class StatsController @Inject()(
    val authService: Authenticator,
    val service: StatsService
) extends MusitController {

  val logger = Logger(classOf[StatsController])

  /**
   * TODO: Document me!
   */
  def stats(
      mid: Int,
      nodeId: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.nodeStats(mid, nodeId)(request.user).map {
      case MusitSuccess(maybeStats) =>
        maybeStats.map { stats =>
          Ok(Json.toJson(stats))
        }.getOrElse {
          NotFound(Json.obj("message" -> s"Could not find nodeId $nodeId"))
        }

      case err: MusitError =>
        logger.error(
          "An unexpected error occured when trying to read " +
            s"node stats for $nodeId. Message was: ${err.message}"
        )
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

}
