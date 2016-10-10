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
import no.uio.musit.microservice.storagefacility.domain.Move
import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveEvent, MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.service.StorageNodeService
import no.uio.musit.service.MusitResults.{MusitError, MusitResult, MusitSuccess, MusitValidationError}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
final class StorageNodeResource @Inject() (
    service: StorageNodeService
) extends Controller {

  val logger = Logger(classOf[StorageNodeResource])

  // TODO: Use user from an enriched request type in a proper SecureAction
  import no.uio.musit.microservice.storagefacility.DummyData.DummyUser

  private def addResult[T <: StorageNode](
    res: MusitResult[Option[T]]
  ): Result = {
    res match {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { n =>
          Created(Json.toJson[T](n))
        }.getOrElse {
          InternalServerError(Json.obj("message" -> "Could not node after insertion"))
        }

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * TODO: Document me!
   */
  def add = Action.async(parse.json) { request =>
    // TODO: Extract current user information from enriched request.
    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        node match {
          case su: StorageUnit =>
            logger.debug(s"Adding a new StorageUnit ${su.name}")
            service.addStorageUnit(su).map(addResult)

          case b: Building =>
            logger.debug(s"Adding a new Building ${b.name}")
            service.addBuilding(b).map(addResult)

          case r: Room =>
            logger.debug(s"Adding a new Room ${r.name}")
            service.addRoom(r).map(addResult)

          case o: Organisation =>
            logger.debug(s"Adding a new Organisation ${o.name}")
            service.addOrganisation(o).map(addResult)

          case bad =>
            val message = s"Wrong service for adding a ${bad.storageType}."
            Future.successful(BadRequest(Json.obj("message" -> message)))
        }

      case err: JsError =>
        val jserr = JsError.toJson(err)
        logger.error(s"Received an invalid JSON:\n${Json.prettyPrint(jserr)}")
        Future.successful(BadRequest(jserr))

    }
  }

  /**
   * TODO: Document me!
   */
  def addRoot = Action.async { implicit request =>
    service.addRoot().map(addResult)
  }

  /**
   * TODO: Document me!
   */
  def root = Action.async { implicit request =>
    service.rootNodes.map(roots => Ok(Json.toJson(roots)))
  }

  /**
   * TODO: Document me!
   */
  def children(id: Long) = Action.async { implicit request =>
    service.getChildren(id).map { nodes =>
      Ok(Json.toJson[Seq[GenericStorageNode]](nodes))
    }
  }

  /**
   * TODO: Document me!
   */
  def getById(id: Long) = Action.async { implicit request =>
    service.getNodeById(id).map {
      case MusitSuccess(maybeNode) =>
        maybeNode.map(n => Ok(Json.toJson[StorageNode](n))).getOrElse(NotFound)

      case musitError: MusitError =>
        musitError match {
          case MusitValidationError(message, exp, act) =>
            BadRequest(Json.obj("message" -> message))

          case internal: MusitError =>
            InternalServerError(Json.obj("message" -> internal.message))
        }
    }
  }

  /**
   * TODO: Document me!
   */
  def update(id: Long) = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        val futureRes: Future[MusitResult[Option[StorageNode]]] = node match {
          case su: StorageUnit => service.updateStorageUnit(id, su)
          case b: Building => service.updateBuilding(id, b)
          case r: Room => service.updateRoom(id, r)
          case o: Organisation => service.updateOrganisation(id, o)
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

  }

  /**
   * TODO: Document me!
   */
  def delete(id: Long) = Action.async { implicit request =>
    // TODO: Extract current user information from enriched request.
    service.deleteNode(id).map {
      case MusitSuccess(maybeDeleted) =>
        maybeDeleted.map { numDeleted =>
          if (numDeleted == -1) {
            BadRequest(Json.obj("message" -> s"Node $id is not empty"))
          } else {
            Ok(Json.obj("message" -> s"Deleted $numDeleted storage nodes."))
          }
        }.getOrElse {
          NotFound(Json.obj(
            "message" -> s"Could not find storage node with id: $id"
          ))
        }

      case err: MusitError =>
        logger.error("An unexpected error occured when trying to delete a node " +
          s"with ID $id. Message was: ${err.message}")
        InternalServerError(Json.obj("message" -> err.message))
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
      val failed = mru.filter(_._2.isFailure).map(_._1)

      if (success.isEmpty) {
        BadRequest(Json.obj("message" -> "Nothing was moved"))
      } else {
        logger.debug(s"Moved: ${success.mkString("[", ", ", "]")}, " +
          s"failed: ${failed.mkString("[", ", ", "]")}")
        Ok(Json.obj(
          "moved" -> success,
          "failed" -> failed
        ))
      }

    }
  }

  /**
   * TODO: Document me!
   *
   * TODO: Check if moving to self...should be illegal!
   */
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

  /**
   * TODO: Document me!
   */
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

  /**
   * Endpoint for retrieving the {{{limit}}} number of past move events.
   *
   * @param objectId the objectId to get move history for.
   * @param limit Int indicating the number of results to return.
   * @return A JSON array with the {{{limi}}} number of move events.
   */
  def objectLocationHistory(objectId: Long, limit: Int) = Action.async { implicit request =>
    service.objectLocationHistory(objectId, Option(limit)).map {
      case MusitSuccess(history) => Ok(Json.toJson(history))
      case err: MusitError => InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * TODO: Document me!
   */
  def stats(nodeId: Long) = Action.async { implicit request =>
    service.nodeStats(nodeId).map {
      case MusitSuccess(maybeStats) =>
        maybeStats.map { stats =>
          Ok(Json.toJson(stats))
        }.getOrElse {
          NotFound(Json.obj("message" -> s"Could not find nodeId $nodeId"))
        }

      case err: MusitError =>
        logger.error("An unexpected error occured when trying to read " +
          s"node stats for $nodeId. Message was: ${err.message}")
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  def CurrentObjectLocation(mid: Int, oid: Long) = Action.async { implicit request =>
    ???
  }
}

