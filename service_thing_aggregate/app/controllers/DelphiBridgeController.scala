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
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions.Read
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.json._
import services.{ObjectService, StorageNodeService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class DelphiBridgeController @Inject() (
    val authService: Authenticator,
    val nodeService: StorageNodeService,
    val objService: ObjectService
) extends MusitController {

  val logger = Logger(classOf[DelphiBridgeController])

  /**
   * Returns the StorageNodeDatabaseId and name for an objects current location.
   */
  def currentNode(
    oldObjectId: Long,
    schemaName: String
  ) = MusitSecureAction().async { implicit request =>
    nodeService.currNodeForOldObject(oldObjectId, schemaName)(request.user).map {
      case MusitSuccess(mres) =>
        mres.map { res =>
          Ok(Json.obj(
            "nodeId" -> Json.toJson(res._1),
            "currentLocation" -> res._2
          ))
        }.getOrElse {
          NotFound(Json.obj(
            "message" -> (s"Could not find current node for object $oldObjectId " +
              s"in schema $schemaName")
          ))
        }

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Endpoint that returns all the nodes under a museums external root nodes.
   */
  def outsideNodes(mid: Int) = MusitSecureAction(Read).async { implicit request =>
    nodeService.nodesOutsideMuseum(mid).map {
      case MusitSuccess(res) =>
        val jsSeq = JsArray(
          res.map(sn => Json.obj(
            "nodeId" -> Json.toJson(sn._1),
            "name" -> sn._2
          ))
        )
        Ok(jsSeq)

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  case class TranslateIdRequest(schemaName: String, oldObjectIds: Seq[Long])

  object TranslateIdRequest {
    implicit val reads: Reads[TranslateIdRequest] = Json.reads[TranslateIdRequest]
  }

  /**
   * Endpoint for converting old object IDs from the old MUSIT database schemas
   * to an objectId recognized by the new system.
   */
  def translateOldObjectIds = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[TranslateIdRequest] match {
      case JsSuccess(trans, _) =>
        objService.findByOldObjectIds(trans.schemaName, trans.oldObjectIds).map {
          case MusitSuccess(res) => Ok(Json.toJson(res))
          case err: MusitError => InternalServerError(Json.obj("message" -> err.message))
        }

      case jsErr: JsError =>
        Future.successful(BadRequest(JsError.toJson(jsErr)))
    }
  }

}
