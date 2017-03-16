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

          logger.debug(
            s"Redirecting to: ${web.routes.Dashboard.index().absoluteURL()}"
          )

          Redirect(
            url = web.routes.Dashboard.index().absoluteURL(),
            queryString = Map("_at" -> Seq(token))
          )
        }
      }
      .getOrElse {
        Unauthorized(Json.obj("message" -> "Access denied."))
      }
  }

}
