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
import no.uio.musit.models.MuseumId
import no.uio.musit.security.{AuthenticatedUser, Authenticator}
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{ObjectAggregationService, StorageNodeService}

import scala.concurrent.Future

class ObjectAggregationController @Inject() (
    val authService: Authenticator,
    val service: ObjectAggregationService,
    val storageNodeService: StorageNodeService
) extends MusitController {

  val logger = Logger(classOf[ObjectAggregationController])

  def getObjects(
    mid: Int,
    nodeId: Long
  ) = MusitSecureAction(mid, Read).async { request =>
    storageNodeService.nodeExists(mid, nodeId).flatMap {
      case MusitSuccess(true) =>
        getObjectsByNodeId(mid, nodeId)(request.user)

      case MusitSuccess(false) =>
        Future.successful(NotFound(Json.obj(
          "message" -> s"Did not find node in museum $mid with nodeId $nodeId"
        )))

      case MusitDbError(msg, ex) =>
        logger.error(msg, ex.orNull)
        Future.successful(InternalServerError(Json.obj("message" -> msg)))

      case r: MusitError =>
        Future.successful(InternalServerError(Json.obj("message" -> r.message)))
    }
  }

  private def getObjectsByNodeId(
    mid: MuseumId,
    nodeId: Long
  )(implicit currUsr: AuthenticatedUser): Future[Result] = {
    service.getObjects(mid, nodeId).map {
      case MusitSuccess(objects) =>
        Ok(Json.toJson(objects))

      case MusitDbError(msg, ex) =>
        logger.error(msg, ex.orNull)
        InternalServerError(Json.obj("message" -> msg))

      case r: MusitError =>
        InternalServerError(Json.obj("message" -> r.message))
    }
  }

}
