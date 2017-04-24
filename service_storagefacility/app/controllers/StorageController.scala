package controllers

import com.google.inject.Inject
import models.MovableObject
import models.Move.{DelphiMove, MoveNodesCmd, MoveObjectsCmd}
import models.event.move.{MoveNode, MoveObject}
import models.storage._
import no.uio.musit.MusitResults._
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.security.Authenticator
import no.uio.musit.security.Permissions._
import no.uio.musit.service.MusitController
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import services.StorageNodeService

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
final class StorageController @Inject()(
    val authService: Authenticator,
    val service: StorageNodeService
) extends MusitController {

  val logger = Logger(classOf[StorageController])

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
  ) = MusitSecureAction(mid, StorageFacility, Admin).async(parse.json) { implicit request =>
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
  ) = MusitSecureAction(mid, StorageFacility, GodMode).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[RootNode] match {
      case JsSuccess(root, _) => service.addRoot(mid, root).map(addResult)
      case err: JsError       => Future.successful(BadRequest(JsError.toJson(err)))
    }
  }

  /**
   * TODO: Document me!
   */
  def root(
      mid: Int
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
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
   * TODO: Document me!
   */
  def children(
      mid: Int,
      id: Long,
      page: Int,
      limit: Int
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
    service.getChildren(mid, id, page, limit).map {
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

  /**
   * TODO: Document me!
   */
  def getById(
      mid: Int,
      id: Long
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
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

  def getByStorageNodeId(mid: Int, nodeId: Option[String]): Future[Result] = {
    nodeId
      .flatMap(StorageNodeId.fromString)
      .map { nid =>
        service.getNodeByStorageNodeId(mid, nid).map {
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
      .getOrElse {
        Future.successful {
          BadRequest(Json.obj("message" -> s"Invalid UUID $nodeId"))
        }
      }
  }

  def getByOldBarcode(mid: Int, oldBarcode: Option[Long]): Future[Result] = {
    oldBarcode.map { barcode =>
      service.getNodeByOldBarcode(mid, barcode).map {
        case MusitSuccess(maybeNode) =>
          maybeNode.map(node => Ok(Json.toJson(node))).getOrElse(NoContent)

        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful {
        BadRequest(Json.obj("message" -> s"oldBarcode did not contain a value"))
      }
    }
  }

  /**
   * Service for looking up a storage node based on UUID.
   */
  def scan(
      mid: Int,
      storageNodeId: Option[String],
      oldBarcode: Option[Long]
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
    if (storageNodeId.nonEmpty) getByStorageNodeId(mid, storageNodeId)
    else if (oldBarcode.nonEmpty) getByOldBarcode(mid, oldBarcode)
    else
      Future.successful {
        BadRequest(
          Json.obj(
            "message" -> "Either storage node id or old barcode must be specified"
          )
        )
      }
  }

  /**
   * TODO: Document me!
   */
  def update(
      mid: Int,
      id: Long
  ) = MusitSecureAction(mid, StorageFacility, Admin).async(parse.json) { implicit request =>
    implicit val currUsr = request.user

    request.body.validate[StorageNode] match {
      case JsSuccess(node, _) =>
        val futureRes: Future[MusitResult[Option[StorageNode]]] = node match {
          case su: StorageUnit => service.updateStorageUnit(mid, id, su)
          case b: Building     => service.updateBuilding(mid, id, b)
          case r: Room         => service.updateRoom(mid, id, r)
          case o: Organisation => service.updateOrganisation(mid, id, o)
          case _               => Future.successful(MusitSuccess(None))
        }

        futureRes.map { musitRes =>
          musitRes.map {
            case Some(updated) => Ok(Json.toJson(updated))
            case None          => NotFound

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
  ) = MusitSecureAction(mid, StorageFacility, Admin).async { implicit request =>
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

  /**
   * Moves one or more nodes from one parent node to another. Upholding the
   * rules of the node hierarchy. The response contains a list of the nodes
   * that were moved successfully, and the nodes that did not get moved for
   * some reason.
   */
  def moveNode(
      mid: Int
  ) = MusitSecureAction(mid, StorageFacility, Write).async(parse.json) { implicit request =>
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
          s"Error parsing JSON:" +
            s"\n${Json.prettyPrint(JsError.toJson(error))}"
        )
        Future.successful(BadRequest(JsError.toJson(error)))
    }
  }

  /**
   * Moves one or more objects to a node in the hierarchy. The response includes
   * a list of successfully moved objects, and the objects that failed for some
   * reason.
   */
  def moveObject(
      mid: Int
  ) = MusitSecureAction(mid, StorageFacility, Write).async(parse.json) { implicit request =>
    implicit val currUser = request.user

    val js = request.body

    js.validate[MoveObjectsCmd].orElse(js.validate[DelphiMove]) match {
      case JsSuccess(cmd, _) =>
        val events = MoveObject.fromCommand(currUser.id, cmd)
        service.moveObjects(mid, cmd.destination, events).map {
          case MusitSuccess(oids) =>
            cmd match {
              case m: MoveObjectsCmd =>
                val success = m.items.filter(mo => oids.contains(mo.id))
                val failed  = m.items.filterNot(mo => oids.contains(mo.id))
                Ok(
                  Json.obj(
                    "moved"  -> Json.toJson(success),
                    "failed" -> Json.toJson(failed)
                  )
                )

              case d: DelphiMove =>
                val failed = d.items.filterNot(oids.contains)
                Ok(
                  Json.obj(
                    "moved"  -> oids.map(_.underlying),
                    "failed" -> failed.map(_.underlying)
                  )
                )
            }

          case MusitValidationError(msg, _, _) =>
            BadRequest(Json.obj("message" -> msg))

          case err: MusitError =>
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
   * Endpoint for retrieving the {{{limit}}} number of past move events.
   *
   * @param mid      MuseumId
   * @param objectId the objectId to get move history for.
   * @param objectType the type of object expected to find location history for
   * @param limit    Int indicating the number of results to return.
   * @return A JSON array with the {{{limit}}} number of move events.
   */
  def objectLocationHistory(
      mid: Int,
      objectId: Long,
      objectType: String,
      limit: Int
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
    service.objectLocationHistory(mid, objectId, Option(limit)).map {
      case MusitSuccess(history) =>
        Ok(Json.toJson(history))

      case err: MusitError =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Get the current location for a an ObjectId.
   *
   * @param mid MuseumId
   * @param oid Long (must be a valid ObjectId)
   * @param objectType the type of object expected find location history for
   * @return a JSON response with the StorageNode where the object is located.
   */
  def currentObjectLocation(
      mid: Int,
      oid: Long,
      objectType: String
  ) = MusitSecureAction(mid, StorageFacility, Read).async { implicit request =>
    ObjectType
      .fromString(objectType)
      .map { ot =>
        service.currentObjectLocation(mid, oid, ot).map {
          case MusitSuccess(optCurrLoc) =>
            optCurrLoc.map { currLoc =>
              Ok(Json.toJson(currLoc))
            }.getOrElse {
              NotFound(
                Json.obj("message" -> s"Could not find objectId $oid in museum $mid")
              )
            }

          case err: MusitError =>
            logger.error(
              "An unexpected error occurred when trying to read " +
                s"currentLocation for object $oid. Message was: ${err.message}"
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

  /**
   * Get the current location for a list of ObjectIds
   *
   * @param mid MuseumId
   * @return A JSON response with a list of StorageNodes.
   */
  def currentObjectLocations(
      mid: Int
  ) = MusitSecureAction(mid, StorageFacility, Read).async(parse.json) { implicit request =>
    request.body.validate[Seq[MovableObject]] match {
      case JsSuccess(objs, _) =>
        service.currentObjectLocations(mid, objs).map {
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

  def search(
      mid: Int,
      searchStr: Option[String],
      page: Int,
      limit: Int
  ) = MusitSecureAction(mid, StorageFacility, Read).async { request =>
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
