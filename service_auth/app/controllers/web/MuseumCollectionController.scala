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

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.{Authenticator, EncryptedToken}
import no.uio.musit.service.MusitAdminController
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.AuthDao

class MuseumCollectionController @Inject() (
    implicit
    val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao
) extends MusitAdminController {

  def listCollections = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    dao.allCollections.map {
      case MusitSuccess(cols) => Ok(views.html.collections(encTok, cols))
      case err: MusitError => InternalServerError(views.html.error(encTok, err.message))
    }
  }

}
