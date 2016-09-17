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
package no.uio.musit.microservice.storagefacility.resource

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.domain.MusitResults.{ MusitError, MusitResult, MusitSuccess, MusitValidationError }
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.service.StorageNodeService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
final class StorageUnitResource @Inject() (
    storageUnitService: StorageNodeService
) extends Controller {

  val logger = Logger(classOf[StorageUnitResource])

  /**
   * TODO: Document me!
   */
  def add = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        val futureNode = node match {
          case su: StorageUnit => storageUnitService.addStorageUnit(su)
          case b: Building => storageUnitService.addBuilding(b)
          case r: Room => storageUnitService.addRoom(r)
          case o: Organisation => storageUnitService.addOrganisation(o)
        }
        futureNode.map(node => Created(Json.toJson(node)))

      case err: JsError =>
        Future.successful(BadRequest(JsError.toJson(err)))

    }
  }

  /**
   * TODO: Document me!
   */
  def getChildren(id: Long) = Action.async { implicit request =>
    storageUnitService.getChildren(id).map(nodes => Ok(Json.toJson(nodes)))
  }

  /**
   * TODO: Document me!
   */
  def getById(id: Long) = Action.async { implicit request =>
    storageUnitService.getNodeById(id).map {
      case MusitSuccess(maybeNode) =>
        maybeNode.map(node => Ok(Json.toJson(maybeNode))).getOrElse(NotFound)

      case musitError: MusitError[_] =>
        musitError match {
          case MusitValidationError(message, exp, act) =>
            BadRequest(Json.obj("message" -> message))

          case internal: MusitError[_] =>
            InternalServerError(Json.obj("message" -> internal.message))
        }
    }
  }

  /**
   * TODO: Document me!
   */
  def update(id: Long) = Action.async(parse.json) { implicit request =>
    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        val futureRes: Future[MusitResult[Option[StorageNode]]] = node match {
          case su: StorageUnit => storageUnitService.updateStorageUnit(id, su)
          case b: Building => storageUnitService.updateBuilding(id, b)
          case r: Room => storageUnitService.updateRoom(id, r)
          case o: Organisation => storageUnitService.updateOrganisation(id, o)
        }

        futureRes.map { musitRes =>
          musitRes.map {
            case Some(updated) => Ok(Json.toJson(updated))
            case None => NotFound

          }.getOrElse {
            InternalServerError(
              Json.obj(
                "message" -> s"An unexpected error occured while trying to update StorageNode with ID $id"
              )
            )
          }
        }

      case JsError(error) =>
        Future.successful(
          BadRequest(JsError.toJson(error))
        )
    }

  }

  /**
   * TODO: Document me!
   */
  def delete(id: Long) = Action.async {
    storageUnitService.deleteNode(id).map {
      case MusitSuccess(numDeleted) =>
        if (numDeleted == 0) {
          NotFound(Json.obj(
            "message" -> s"Could not find storage node with id: $id"
          ))
        } else {
          Ok(Json.obj("message" -> s"Deleted $numDeleted storage nodes."))
        }

      case err: MusitError[_] =>
        logger.error("An unexpected error occured when trying to delete a node " +
          s"with ID $id. Message was: ${err.message}")
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

}

