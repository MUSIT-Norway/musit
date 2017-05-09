package controllers.storage

import com.google.inject.Inject
import models.storage.MovableObject
import models.storage.Move.{DelphiMoveCmd, MoveNodesCmd, MoveObjectsCmd}
import models.storage.event.move._
import models.storage.nodes._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.security.Permissions._
import no.uio.musit.security.{AuthenticatedUser, Authenticator}
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services.objects.ObjectService
import services.storage.StorageNodeService

import scala.concurrent.Future

/**
 * Controller that exposes service endpoints to interact with data related to
 * storage nodes in the storage facility module.
 */
final class StorageController @Inject()(
    val authService: Authenticator,
    val service: StorageNodeService,
    val objService: ObjectService
) extends MusitController {

  val logger = Logger(classOf[StorageController])

  /**
   * Helper function implementing shared logic for handling the result when
   * adding a new {{{StorageNode}}}.
   *
   * @param res The {{{MusitResult[Option[T]]}}} to handle
   * @tparam T The type of the data inside the result
   * @return A play Result.
   */
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
   * Endpoint for adding a new StorageNode. Takes a {{{StorageNode}}} as input
   * in the request body, and returns the created node in the response.
   *
   * @see [[models.storage.nodes.StorageNode]]
   */
  def add(
      mid: Int
  ) = MusitSecureAction(mid, Admin).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        service.addNode(mid, node).map(addResult)

      case err: JsError =>
        val jserr = JsError.toJson(err)
        logger.error(s"Received an invalid JSON:\n${Json.prettyPrint(jserr)}")
        Future.successful(BadRequest(jserr))
    }
  }

  /**
   * Adds a new RootNode to the given {{{MuseumId}}}. Takes a {{{RootNode}}} as
   * input in the request body, and returns the created root node in the
   * response.
   *
   * @see [[models.storage.nodes.RootNode]]
   */
  def addRoot(
      mid: Int
  ) = MusitSecureAction(mid, GodMode).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[RootNode] match {
      case JsSuccess(root, _) => service.addRoot(mid, root).map(addResult)
      case err: JsError       => Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  /**
   * Fetches the {{{RootNode}}}s for the 'given MuseumId. Returns either a JSON
   * array of {{{RootNode}}}s or an empty array if none were found.
   *
   * @see [[models.storage.nodes.RootNode]]
   */
  def root(mid: Int) = MusitSecureAction(mid, Read).async { implicit request =>
    service.rootNodes(mid).map {
      case MusitSuccess(roots) =>
        Ok(Json.toJson[Seq[StorageNode]](roots))

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
   * Endpoint for fetching the children for the given {{{StorageNodeId}}}. The
   * result is paged based on the value of the {{{limit}}} argument (defaults to
   * 25 results per page).
   *
   * Returns a result containing a JSON object with the total number of matches,
   * and an array of storage nodes.
   */
  def children(
      mid: Int,
      id: String,
      page: Int,
      limit: Int
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    StorageNodeId
      .fromString(id)
      .map { nid =>
        service.getChildren(mid, nid, page, limit).map {
          case MusitSuccess(nodes) =>
            Ok(
              Json.obj(
                "totalMatches" -> nodes.totalMatches,
                "matches"      -> Json.toJson[Seq[GenericStorageNode]](nodes.matches)
              )
            )
          case musitError: MusitError =>
            InternalServerError(Json.obj("message" -> musitError.message))
        }
      }
      .getOrElse(invaludUuidResponse(id))
  }

  private def getByStorageNodeId(mid: Int, nodeId: String): Future[Result] = {
    StorageNodeId
      .fromString(nodeId)
      .map { nid =>
        service.getNodeById(mid, nid).map {
          case MusitSuccess(maybeNode) =>
            maybeNode.map(node => Ok(Json.toJson(node))).getOrElse {
              NotFound(
                Json.obj(
                  "message" -> s"Could not find node with UUID $nodeId"
                )
              )
            }

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(nodeId))
  }

  private def getByOldBarcode(mid: Int, barcode: Long): Future[Result] = {
    service.getNodeByOldBarcode(mid, barcode).map {
      case MusitSuccess(maybeNode) =>
        maybeNode.map(node => Ok(Json.toJson(node))).getOrElse(NoContent)

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Fetch the StorageNode with the provided StorageNodeId (UUID) in the given
   * MuseumId.
   */
  def getById(
      mid: Int,
      id: String
  ) = MusitSecureAction(mid, Read).async(getByStorageNodeId(mid, id))

  /**
   * Service for looking up a storage node based on UUID.
   */
  def scan(
      mid: Int,
      storageNodeId: Option[String],
      oldBarcode: Option[Long]
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    storageNodeId
      .map(sid => getByStorageNodeId(mid, sid))
      .orElse(oldBarcode.map(bc => getByOldBarcode(mid, bc)))
      .getOrElse {
        Future.successful {
          BadRequest(
            Json.obj(
              "message" -> "Either storage node id or old barcode must be specified"
            )
          )
        }
      }
  }

  /**
   * Update the StorageNode with the given id in the specified museum.
   */
  def update(
      mid: Int,
      id: String
  ) = MusitSecureAction(mid, Admin).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    StorageNodeId
      .fromString(id)
      .map { sid =>
        request.body.validate[StorageNode] match {
          case JsSuccess(node, _) =>
            service.updateNode(mid, sid, node).map {
              case MusitSuccess(maybeUpdated) =>
                maybeUpdated.map(upd => Ok(Json.toJson(upd))).getOrElse(NotFound)

              case err: MusitError =>
                InternalServerError(Json.obj("message" -> err.message))
            }
          case JsError(error) =>
            Future.successful(BadRequest(JsError.toJson(error)))
        }
      }
      .getOrElse(invaludUuidResponse(id))
  }

  /**
   * Endpoint for marking a StorageNode as deleted.
   */
  def delete(
      mid: Int,
      id: String
  ) = MusitSecureAction(mid, Admin).async { implicit request =>
    implicit val currUsr = request.user
    StorageNodeId
      .fromString(id)
      .map { sid =>
        service.deleteNode(mid, sid).map {
          case MusitSuccess(maybeDeleted) =>
            maybeDeleted.map { numDeleted =>
              if (numDeleted == -1) {
                BadRequest(Json.obj("message" -> s"Node $id is not empty"))
              } else {
                Ok(Json.obj("message" -> s"Deleted $numDeleted storage nodes."))
              }
            }.getOrElse {
              NotFound(
                Json.obj(
                  "message" -> s"Could not find storage node with id: $id"
                )
              )
            }

          case err: MusitError =>
            logger.error(
              "An unexpected error occurred when trying to delete a " +
                s"node with ID $id. Message was: ${err.message}"
            )
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(id))
  }

  /**
   * Moves one or more nodes from one parent node to another. Upholding the
   * rules of the node hierarchy. The response contains a list of the nodes
   * that were moved successfully, and the nodes that did not get moved for
   * some reason.
   */
  def moveNode(
      mid: Int
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[MoveNodesCmd] match {
      case JsSuccess(cmd, _) =>
        val events = MoveNode.fromCommand(request.user.id, cmd)
        service.moveNodes(mid, cmd.destination, events).map {
          case MusitSuccess(nids) =>
            val failed = cmd.items.filterNot(nids.contains)
            Ok(
              Json.obj(
                "moved"  -> nids.map(_.underlying),
                "failed" -> failed.map(_.underlying)
              )
            )

          case MusitValidationError(msg, _, _) =>
            BadRequest(Json.obj("message" -> msg))

          case err: MusitError =>
            InternalServerError(Json.obj("messsage" -> err.message))
        }

      case JsError(error) =>
        logger.warn(
          s"Error parsing JSON:\n${Json.prettyPrint(JsError.toJson(error))}"
        )
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * Helper method with logic for executing an actual move on objects.
   */
  private def mvObjects[ResId](
      mid: MuseumId,
      dest: StorageNodeId,
      events: Seq[MoveObject]
  )(
      successFailed: Seq[ObjectUUID] => (Seq[ResId], Seq[ResId])
  )(implicit currUser: AuthenticatedUser, w: Writes[ResId]) = {
    service.moveObjects(mid, dest, events).map {
      case MusitSuccess(oids) =>
        val (success, failed) = successFailed(oids)
        Ok(
          Json.obj(
            "moved"  -> Json.toJson(success),
            "failed" -> Json.toJson(failed)
          )
        )

      case MusitValidationError(msg, _, _) =>
        BadRequest(Json.obj("message" -> msg))

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Moves one or more objects to a node in the hierarchy. The response includes
   * a list of successfully moved objects, and the objects that failed for some
   * reason.
   */
  def moveObject(
      mid: Int
  ) = MusitSecureAction(mid, Write).async(parse.json) { implicit request =>
    implicit val currUser = request.user

    val js = request.body

    val jsRes = js.validate[MoveObjectsCmd].orElse(js.validate[DelphiMoveCmd])

    jsRes match {
      case JsSuccess(cmd: MoveObjectsCmd, _) =>
        val events = MoveObject.fromCommand(currUser.id, cmd)
        mvObjects(mid, cmd.destination, events) { oids =>
          val mobs = cmd.items.map(_.id)
          (mobs.filter(oids.contains), mobs.filterNot(oids.contains))
        }

      case JsSuccess(cmd: DelphiMoveCmd, _) =>
        moveObjectForDelphi(mid, cmd)

      case JsSuccess(bad, path) =>
        Future.successful(BadRequest(Json.obj("message" -> "Unknown command.")))

      case JsError(error) =>
        logger.warn(
          s"Error parsing JSON:" +
            s"\n${Json.prettyPrint(JsError.toJson(error))}"
        )
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * Method for handling moveObject service calls from the Delphi clients.
   * They are still using the "old" API for this, so we need to find the
   * relevant data for this particular type of command.
   *
   * This includes the following steps:
   *
   * 1. Find the correct UUID for the destination node
   * 2. For each objectId (database ID) find their UUID.
   * 3. Convert command to a {{{MoveObjectsCmd}}}
   * 4. Call the {{{mvObjects}}} method defined above
   */
  private def moveObjectForDelphi(
      mid: MuseumId,
      cmd: DelphiMoveCmd
  )(implicit currUser: AuthenticatedUser): Future[Result] = {
    val res = for {
      maybeNode <- MusitResultT(service.getNodeByDatabaseId(mid, cmd.destination))
      idTuples  <- MusitResultT(objService.getUUIDsFor(cmd.items))
    } yield {
      maybeNode.map { node =>
        val dest = node.nodeId.get // safe...since we got the node from the DB.
        val mobs =
          idTuples.map(_._2).map(i => MovableObject(i, ObjectTypes.CollectionObject))
        val events = MoveObject.fromCommand(currUser.id, MoveObjectsCmd(dest, mobs))

        (idTuples, dest, events)
      }
    }
    res.value.flatMap {
      case MusitSuccess(maybeTuple) =>
        maybeTuple.map { tuple =>
          mvObjects(mid, tuple._2, tuple._3) { oids =>
            (
              tuple._1.filter(t => oids.contains(t._2)).map(_._1),
              tuple._1.filterNot(t => oids.contains(t._2)).map(_._1)
            )
          }
        }.getOrElse {
          Future.successful {
            BadRequest(
              Json.obj(
                "message" -> s"Could not find destination node ${cmd.destination}"
              )
            )
          }
        }

      case err: MusitError =>
        Future.successful(InternalServerError(Json.obj("message" -> err.message)))
    }
  }

  /**
   * Endpoint for retrieving the {{{limit}}} number of past move events.
   */
  def objectLocationHistory(
      mid: Int,
      objectId: String,
      objectType: String,
      limit: Int
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    ObjectUUID
      .fromString(objectId)
      .map { oid =>
        service.objectLocationHistory(mid, oid, Option(limit)).map {
          case MusitSuccess(history) =>
            Ok(Json.toJson(history))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      }
      .getOrElse(invaludUuidResponse(objectId))
  }

  /**
   * Get the current location for a an ObjectId.
   */
  def currentObjectLocation(
      mid: Int,
      objectId: String,
      objectType: String
  ) = MusitSecureAction(mid, Read).async { implicit request =>
    ObjectUUID
      .fromString(objectId)
      .map { oid =>
        ObjectType
          .fromString(objectType)
          .map { ot =>
            service.currentObjectLocation(mid, oid, ot).map {
              case MusitSuccess(optCurrLoc) =>
                optCurrLoc.map { currLoc =>
                  Ok(Json.toJson(currLoc))
                }.getOrElse {
                  NotFound(
                    Json.obj(
                      "message" -> s"Could not find objectId $objectId in museum $mid"
                    )
                  )
                }

              case err: MusitError =>
                logger.error(
                  "An unexpected error occurred when trying to read " +
                    s"currentLocation for object $objectId. Message was: ${err.message}"
                )
                InternalServerError(Json.obj("message" -> err.message))
            }
          }
          .getOrElse {
            Future.successful(
              BadRequest(Json.obj("message" -> s"Not a valid object type $objectType"))
            )
          }
      }
      .getOrElse(invaludUuidResponse(objectId))
  }

  /**
   * Get the current location for a list of {{{MovableObjects}}}
   */
  def currentObjectLocations(
      mid: Int
  ) = MusitSecureAction(mid, Read).async(parse.json) { implicit request =>
    request.body.validate[Seq[MovableObject]] match {
      case JsSuccess(objs, _) =>
        service.currentObjectLocations(mid, objs.map(_.id)).map {
          case MusitSuccess(objectsLocations) =>
            Ok(Json.toJson(objectsLocations))

          case err: MusitError =>
            logger.error(
              "An unexpected error occurred when trying to get " +
                s"current location for a list of ${objs.size} objectIds. " +
                s"Message was: ${err.message}"
            )
            InternalServerError(Json.obj("message" -> err.message))
        }

      case JsError(error) =>
        logger.warn(
          s"Error parsing JSON:" +
            s"\n${Json.prettyPrint(JsError.toJson(error))}"
        )
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * Service endpoint exposing "simple" search functionality for StorageNodes.
   */
  def search(
      mid: Int,
      searchStr: Option[String],
      page: Int,
      limit: Int
  ) = MusitSecureAction(mid, Read).async { request =>
    searchStr match {
      case Some(criteria) if criteria.length >= 3 =>
        service.searchByName(mid, criteria, page, limit).map {
          case MusitSuccess(mr) =>
            Ok(Json.toJson(mr))

          case validationError: MusitValidationError =>
            BadRequest(Json.obj("message" -> validationError.message))

          case r: MusitError =>
            InternalServerError(Json.obj("message" -> r.message))
        }

      case Some(_) =>
        Future.successful(
          BadRequest(
            Json.obj(
              "message" -> s"Search requires at least three characters"
            )
          )
        )

      case None =>
        Future.successful(
          BadRequest(
            Json.obj(
              "message" -> s"Search requires at least three characters"
            )
          )
        )
    }
  }

}
