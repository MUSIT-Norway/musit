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
import no.uio.musit.microservice.storagefacility.domain.{ Move, Museum }
import no.uio.musit.microservice.storagefacility.domain.event.move.{ MoveEvent, MoveNode, MoveObject }
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.service.StorageNodeService
import no.uio.musit.service.MusitResults.{ MusitError, MusitResult, MusitSuccess, MusitValidationError }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
final class StorageUnitResource @Inject() (
    service: StorageNodeService
) extends Controller {

  val logger = Logger(classOf[StorageUnitResource])

  // TODO: Use user from an enriched request type in a proper SecureAction
  import no.uio.musit.microservice.storagefacility.DummyData.DummyUser

  /**
   * TODO: Document me!
   */
  def add(mid: Int) = Action.async(parse.json) { request =>
    // TODO: Extract current user information from enriched request.
    Museum.fromMuseumId(mid).map { museumId =>
      request.body.validate[StorageNode] match {
        case JsSuccess(node, _) =>
          node match {
            case su: StorageUnit =>
              logger.debug(s"Adding a new StorageUnit ${su.name}")
              service.addStorageUnit(mid, su).map { n =>
                Created(Json.toJson[StorageNode](n))
              }

            case b: Building =>
              logger.debug(s"Adding a new Building ${b.name}")
              service.addBuilding(mid, b).map { n =>
                Created(Json.toJson[StorageNode](n))
              }

            case r: Room =>
              logger.debug(s"Adding a new Room ${r.name}")
              service.addRoom(mid, r).map { n =>
                Created(Json.toJson[StorageNode](n))
              }

            case o: Organisation =>
              logger.debug(s"Adding a new Organisation ${o.name}")
              service.addOrganisation(mid, o).map { n =>
                Created(Json.toJson[StorageNode](n))
              }

            case bad =>
              val message = s"Wrong service for adding a ${bad.storageType}."
              Future.successful(BadRequest(Json.obj("message" -> message)))
          }

        case err: JsError =>
          logger.error(s"Received an invalid JSON")
          Future.successful(BadRequest(JsError.toJson(err)))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def addRoot(mid: Int) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museumId =>
      service.addRoot(mid, Root()).map(node => Created(Json.toJson(node)))
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def root(mid: Int) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museumId =>
      service.rootNodes(mid).map(roots => Ok(Json.toJson(roots)))
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def children(mid: Int, id: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museumId =>
      service.getChildren(mid, id).map { nodes =>
        Ok(Json.toJson[Seq[GenericStorageNode]](nodes))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def getById(mid: Int, id: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museumId =>
      service.getNodeById(mid, id).map {
        case MusitSuccess(maybeNode) =>
          maybeNode.map { node =>
            logger.debug(s"RETURNING NODE $node")
            Ok(Json.toJson[StorageNode](node))
          }.getOrElse(NotFound)

        case musitError: MusitError =>
          musitError match {
            case MusitValidationError(message, exp, act) =>
              BadRequest(Json.obj("message" -> message))

            case internal: MusitError =>
              InternalServerError(Json.obj("message" -> internal.message))
          }
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def update(mid: Int, id: Long) = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    Museum.fromMuseumId(mid).map { museumId =>
      request.body.validate[StorageNode] match {
        case JsSuccess(node, _) =>
          val futureRes: Future[MusitResult[Option[StorageNode]]] = node match {
            case su: StorageUnit => service.updateStorageUnit(mid, id, su)
            case b: Building => service.updateBuilding(mid, id, b)
            case r: Room => service.updateRoom(mid, id, r)
            case o: Organisation => service.updateOrganisation(mid, id, o)
            case notCorrect => Future.successful(MusitSuccess(None))
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
          Future.successful(BadRequest(JsError.toJson(error)))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * TODO: Document me!
   */
  def delete(mid: Int, id: Long) = Action.async { implicit request =>
    // TODO: Extract current user information from enriched request.
    Museum.fromMuseumId(mid).map { museumId =>
      service.deleteNode(mid, id).map {
        case MusitSuccess(numDeleted) =>
          if (numDeleted == 0) {
            NotFound(Json.obj(
              "message" -> s"Could not find storage node with id: $id"
            ))
          } else {
            Ok(Json.obj("message" -> s"Deleted $numDeleted storage nodes."))
          }

        case err: MusitError =>
          logger.error("An unexpected error occured when trying to delete a node " +
            s"with ID $id. Message was: ${err.message}")
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Helper function to encapsulate shared logic in both the different move
   * endpoints.
   */
  private def move[A <: MoveEvent](
    events: Seq[A]
  )(mv: (Long, A) => Future[MusitResult[Long]]): Future[Result] = {
    Future.sequence {
      events.map { e =>
        // We know the affected thing will have an ID since we populated it
        // from the Move command
        val id = e.baseEvent.affectedThing.get.objectId
        mv(id, e).map(res => (id, res))
      }
    }.map { mru =>
      val success = mru.filter(_._2.isSuccess).map(_._1)
      val error = mru.filter(_._2.isFailure).map(_._1)

      if (success.isEmpty) {
        BadRequest(Json.obj("message" -> "Nothing was moved"))
      } else {
        Ok(Json.obj(
          "moved" -> success,
          "failed" -> error
        ))
      }

    }
  }

  def moveNode = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    request.body.validate[Move[StorageNodeId]] match {
      case JsSuccess(cmd, _) =>
        val events = MoveNode.fromCommand(DummyUser, cmd)
        move(events)((id, evt) => service.moveNode(id, evt))

      case JsError(error) =>
        logger.warn(s"Error parsing JSON:\n ${Json.prettyPrint(JsError.toJson(error))}")
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  def moveObject = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    request.body.validate[Move[Long]] match {
      case JsSuccess(cmd, _) =>
        val events = MoveObject.fromCommand(DummyUser, cmd)
        move(events)(service.moveObject)

      case JsError(error) =>
        logger.warn(s"Error parsing JSON:\n ${Json.prettyPrint(JsError.toJson(error))}")
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

}

