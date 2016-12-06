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
import models.Move
import models.event.move.{MoveEvent, MoveNode, MoveObject}
import models.storage._
import no.uio.musit.models.{EventId, MusitId, ObjectId, StorageNodeDatabaseId}
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services.StorageNodeService

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
final class StorageNodeResource @Inject() (
    val authService: Authenticator,
    val service: StorageNodeService
) extends MusitController {

  val logger = Logger(classOf[StorageNodeResource])

  private def addResult[T <: StorageNode](
    res: MusitResult[Option[T]]
  ): Result = {
    res match {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { n =>
          Created(Json.toJson[T](n))
        }.getOrElse {
          val errMsg = "Could not find node after insertion"
          logger.error(errMsg)
          InternalServerError(Json.obj("message" -> errMsg))
        }

      case verr: MusitValidationError =>
        logger.warn(verr.message)
        BadRequest(Json.obj("message" -> verr.message))

      case err: MusitError =>
        logger.error(err.message)
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * TODO: Document me!
   */
  def add(
    mid: Int
  ) = MusitSecureAction(mid, Admin).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        node match {
          case su: StorageUnit =>
            logger.debug(s"Adding a new StorageUnit ${su.name}")
            service.addStorageUnit(mid, su).map(addResult)

          case b: Building =>
            logger.debug(s"Adding a new Building ${b.name}")
            service.addBuilding(mid, b).map(addResult)

          case r: Room =>
            logger.debug(s"Adding a new Room ${r.name}")
            service.addRoom(mid, r).map(addResult)

          case o: Organisation =>
            logger.debug(s"Adding a new Organisation ${o.name}")
            service.addOrganisation(mid, o).map(addResult)

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
  def addRoot(
    mid: Int
  ) = MusitSecureAction(mid, GodMode).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[Root] match {
      case JsSuccess(root, _) => service.addRoot(mid, root).map(addResult)
      case err: JsError => Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  /**
   * TODO: Document me!
   */
  def root(mid: Int) = MusitSecureAction(mid, Read).async { implicit request =>
    service.rootNodes(mid).map(roots => Ok(Json.toJson(roots)))
  }

  /**
   * TODO: Document me!
   */
  def children(
    mid: Int,
    id: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.getChildren(mid, id).map { nodes =>
      Ok(Json.toJson[Seq[GenericStorageNode]](nodes))
    }
  }

  /**
   * TODO: Document me!
   */
  def getById(
    mid: Int,
    id: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.getNodeById(mid, id).map {
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
  def update(
    mid: Int,
    id: Long
  ) = MusitSecureAction(mid, Admin).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

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
                "message" -> ("An unexpected error occurred while trying to " +
                  s"update StorageNode with ID $id")
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
  def delete(
    mid: Int,
    id: Long
  ) = MusitSecureAction(mid, Admin).async { implicit request =>
    implicit val currUsr = request.user

    service.deleteNode(mid, id).map {
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
        logger.error("An unexpected error occurred when trying to delete a " +
          s"node with ID $id. Message was: ${err.message}")
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Helper function to encapsulate shared logic in both the different move
   * endpoints.
   */
  private def move[A <: MoveEvent](
    events: Seq[A]
  )(mv: (MusitId, A) => Future[MusitResult[EventId]]): Future[Result] = {
    Future.sequence {
      events.map { e =>
        // We know the affected thing will have an ID since we populated it
        // from the Move command
        val id = e.affectedThing.get
        mv(id, e).map(res => (id, res))
      }
    }.map { mru =>
      val success = mru.filter(_._2.isSuccess).map(_._1.underlying)
      val failed = mru.filter(_._2.isFailure).map(_._1.underlying)

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
   */
  def moveNode(
    mid: Int
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[Move[StorageNodeDatabaseId]] match {
      case JsSuccess(cmd, _) =>
        val events = MoveNode.fromCommand(request.user.id, cmd)
        move(events)((id, evt) => service.moveNode(mid, id, evt))

      case JsError(error) =>
        logger.warn(s"Error parsing JSON:" +
          s"\n${Json.prettyPrint(JsError.toJson(error))}")
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * TODO: Document me!
   */
  def moveObject(
    mid: Int
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[Move[ObjectId]] match {
      case JsSuccess(cmd, _) =>
        val events = MoveObject.fromCommand(request.user.id, cmd)
        move(events)((id, evt) => service.moveObject(mid, id, evt))

      case JsError(error) =>
        logger.warn(s"Error parsing JSON:" +
          s"\n${Json.prettyPrint(JsError.toJson(error))}")
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * Endpoint for retrieving the {{{limit}}} number of past move events.
   *
   * @param mid      : MuseumId
   * @param objectId the objectId to get move history for.
   * @param limit    Int indicating the number of results to return.
   * @return A JSON array with the {{{limit}}} number of move events.
   */
  def objectLocationHistory(
    mid: Int,
    objectId: Long,
    limit: Int
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.objectLocationHistory(mid, objectId, Option(limit)).map {
      case MusitSuccess(history) =>
        Ok(Json.toJson(history))

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * TODO: Document me!
   */
  def stats(
    mid: Int,
    nodeId: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.nodeStats(mid, nodeId).map {
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

  def currentObjectLocation(
    mid: Int,
    oid: Long
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    service.currentObjectLocation(mid, oid).map {
      case MusitSuccess(optCurrLoc) =>
        optCurrLoc.map { currLoc =>
          Ok(Json.toJson(currLoc))
        }.getOrElse {
          NotFound(Json.obj("message" -> s"Could not find objectId $oid in museum $mid"))
        }

      case err: MusitError =>
        logger.error("An unexpected error occured when trying to read " +
          s" currentLocation for object $oid. Message was: ${err.message}")
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  def search(
    mid: Int,
    searchStr: Option[String],
    page: Int = 1,
    limit: Int = 25
  ) = MusitSecureAction(mid, Read).async { request =>
    searchStr match {
      case Some(criteria) if criteria.length >= 3 =>
        service.searchName(mid, criteria, page, limit).map {
          case MusitSuccess(mr) => Ok(Json.toJson(mr))
          case r: MusitError => InternalServerError(Json.obj("message" -> r.message))
        }

      case Some(criteria) =>
        Future.successful(BadRequest(Json.obj(
          "message" -> s"Search requires at least three characters"
        )))

      case None =>
        Future.successful(BadRequest(Json.obj(
          "message" -> s"Search requires at least three characters"
        )))
    }
  }

}

