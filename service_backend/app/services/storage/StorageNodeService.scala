package services.storage

import com.google.inject.Inject
import models.storage.event.move.{MoveEvent, MoveNode, MoveObject}
import models.storage.nodes._
import models.storage.{FacilityLocation, LocationHistory, ObjectsLocation}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import repositories.musitobject.dao.ObjectDao
import repositories.storage.dao.LocalObjectDao
import repositories.storage.dao.events.MoveDao
import repositories.storage.dao.nodes.{
  BuildingDao,
  OrganisationDao,
  RoomDao,
  StorageUnitDao
}

import scala.concurrent.{ExecutionContext, Future}

class StorageNodeService @Inject()(
    implicit
    val ec: ExecutionContext,
    val unitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val orgDao: OrganisationDao,
    val moveDao: MoveDao,
    val locObjDao: LocalObjectDao,
    val objDao: ObjectDao,
    val envReqService: EnvironmentRequirementService
) extends NodeService
    with OrganisationServiceOps
    with BuildingServiceOps
    with RoomServiceOps
    with StorageUnitServiceOps {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * Simple check to see if a node with the given exists in a museum.
   */
  def exists(
      mid: MuseumId,
      id: StorageNodeId
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Boolean]] = {
    unitDao.exists(mid, id)
  }

  def searchByName(
      mid: MuseumId,
      searchStr: String,
      page: Int,
      limit: Int
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[GenericStorageNode]]] = {
    if (searchStr.length > 2) {
      unitDao.getStorageNodeByName(mid, searchStr, page, limit)
    } else {
      Future.successful {
        MusitValidationError(
          s"Not enough letters in search string $searchStr to look up node by name."
        )
      }
    }
  }

  def addRoot(
      mid: MuseumId,
      root: RootNode
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[RootNode]]] = {
    val theRoot = root
      .assignNodeId()
      .setUpdated(
        by = Some(currUsr.id),
        date = Some(dateTimeNow)
      )

    val res = for {
      nodeId <- MusitResultT(unitDao.insertRoot(mid, theRoot))
      path = NodePath.empty.appendChild(nodeId)
      _    <- MusitResultT(unitDao.setRootPath(nodeId, path))
      node <- MusitResultT(unitDao.findRootNode(nodeId))
    } yield node

    res.value
  }

  def addNode(
      mid: MuseumId,
      node: StorageNode
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    logger.debug(s"Adding a new ${node.storageType.entryName} ${node.name}")
    node.assignNodeId() match {
      case su: StorageUnit => addStorageUnit(mid, su)
      case b: Building     => addBuilding(mid, b)
      case r: Room         => addRoom(mid, r)
      case o: Organisation => addOrganisation(mid, o)
      case bad =>
        val message = s"Wrong service for adding a ${bad.storageType}."
        Future.successful(MusitValidationError(message))
    }
  }

  def updateNode(
      mid: MuseumId,
      nodeId: StorageNodeId,
      node: StorageNode
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    node match {
      case su: StorageUnit => updateStorageUnit(mid, nodeId, su)
      case b: Building     => updateBuilding(mid, nodeId, b)
      case r: Room         => updateRoom(mid, nodeId, r)
      case o: Organisation => updateOrganisation(mid, nodeId, o)
      case _               => Future.successful(MusitSuccess(None))
    }
  }

  def disambiguateAndGet(
      mid: MuseumId,
      maybeIdType: Option[(StorageNodeDatabaseId, StorageType)]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    maybeIdType.map { idt =>
      disambiguateAndGet(mid, idt._1, Some(idt._2))
    }.getOrElse {
      Future.successful(MusitValidationError("No StorageNodeDatabaseId available."))
    }
  }

  def disambiguateAndGet(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      maybeType: Option[StorageType]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    logger.debug(s"Disambiguating StorageType $maybeType")
    maybeType.map {
      case StorageType.RootType =>
        unitDao.findRootNode(id)

      case StorageType.RootLoanType =>
        unitDao.findRootNode(id)

      case StorageType.OrganisationType =>
        getOrganisationByDatabaseId(mid, id)

      case StorageType.BuildingType =>
        getBuildingByDatabaseId(mid, id)

      case StorageType.RoomType =>
        getRoomByDatabaseId(mid, id)

      case StorageType.StorageUnitType =>
        getStorageUnitByDatabaseId(mid, id)

    }.getOrElse {
      logger.warn(s"Could not resolve StorageType $maybeType")
      Future.successful(MusitSuccess(None))
    }
  }

  def getNodeByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    unitDao.getStorageTypeFor(mid, id).flatMap { res =>
      res.map(maybeType => disambiguateAndGet(mid, id, maybeType)).getOrElse {
        logger.debug(s"Node $id not found")
        Future.successful(MusitSuccess(None))
      }
    }
  }

  def getNodeById(
      mid: MuseumId,
      uuid: StorageNodeId
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    unitDao.getStorageTypeFor(mid, uuid).flatMap { res =>
      res.map { maybeIdType =>
        disambiguateAndGet(mid, maybeIdType).map {
          case s: MusitSuccess[Option[StorageNode]] => s
          case v: MusitValidationError              => MusitSuccess(None)
          case e: MusitError                        => e
        }
      }.getOrElse(Future.successful(MusitSuccess(None)))
    }
  }

  def getNodeByOldBarcode(
      mid: MuseumId,
      oldBarcode: Long
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    (for {
      tuple <- MusitResultT(unitDao.getStorageTypeFor(mid, oldBarcode))
      node <- tuple.map(t => MusitResultT(getNodeByDatabaseId(mid, t._1))).getOrElse {
               MusitResultT(
                 Future.successful[MusitResult[Option[StorageNode]]](MusitSuccess(None))
               )
             }
    } yield node).value
  }

  def rootNodes(
      mid: MuseumId
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[RootNode]]] = {
    (for {
      rootNodes <- MusitResultT(unitDao.findRootNodes(mid))
      sortedNodes = rootNodes.sortBy(_.storageType.displayOrder)
    } yield sortedNodes).value
  }

  def getChildren(
      mid: MuseumId,
      id: StorageNodeId,
      page: Int,
      limit: Int
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[PagedResult[GenericStorageNode]]] = {
    unitDao.getChildren(mid, id, page, limit)
  }

  /**
   * returns Some(-1) if the node has children and cannot be removed.
   * returns Some(1) when the node was successfully marked as removed.
   * returns None if the node isn't found.
   */
  def deleteNode(
      mid: MuseumId,
      id: StorageNodeId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Int]]] = {
    unitDao.getById(mid, id).map(_.getOrElse(None)).flatMap {
      case Some(node) =>
        isEmpty(node).flatMap { empty =>
          if (empty) {
            unitDao.markAsDeleted(currUsr.id, mid, id).map(_.map(Some.apply))
          } else {
            Future.successful(MusitSuccess(Some(-1)))
          }
        }

      case None =>
        Future.successful(MusitSuccess(None))
    }
  }

  /**
   * Helper to encapsulate shared logic between the public move methods.
   */
  private def persistMoveEvents[ID <: MusitUUID, E <: MoveEvent](
      mid: MuseumId,
      events: Seq[E]
  )(
      batchInsert: (MuseumId, Seq[E]) => Future[MusitResult[Seq[EventId]]]
  )(
      f: Seq[EventId] => MusitResult[Seq[ID]]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[ID]]] = {
    batchInsert(mid, events).map(_.flatMap(ids => f(ids)))
  }

  private def moveBatchNodes(
      mid: MuseumId,
      affectedNodes: Seq[GenericStorageNode],
      to: GenericStorageNode,
      events: Seq[MoveNode]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[StorageNodeId]]] = {
    logger.debug(s"Destination node is ${to.id} with path ${to.path}")
    logger.debug(s"Filtering away invalid placement of nodes in ${to.id}.")
    // Filter away nodes that didn't pass first round of validation
    val nodesToMove =
      affectedNodes.filter(n => events.exists(_.affectedThing.contains(n.nodeId.get)))
    // Filter away nodes with invalid positions and process the ones remaining
    filterInvalidPosition(mid, to.path, nodesToMove).flatMap { validNodes =>
      if (validNodes.nonEmpty) {
        logger.debug(s"Will move ${validNodes.size} to ${to.id}.")
        // Remove events for which moving the node was identified as invalid.
        val validEvents = events.filter(e => validNodes.exists(_.id == e.affectedThing))

        for {
          // Update the NodePath and partOf for all nodes to be moved.
          resLocUpd <- unitDao.batchUpdateLocation(validNodes, to)
          if resLocUpd.isSuccess
          // If the above update succeeded, we store the move events.
          mvRes <- persistMoveEvents(mid, validEvents)(moveDao.batchInsertNodes) { _ =>
                    MusitSuccess(validNodes.flatMap(_.nodeId))
                  }
        } yield {
          logger.debug(s"Successfully moved ${validNodes.size} nodes to ${to.id}")
          mvRes
        }
      } else {
        Future.successful {
          MusitValidationError("No valid commands were found. No nodes were moved.")
        }
      }
    }
  }

  def moveNodes(
      mid: MuseumId,
      destination: StorageNodeId,
      moveEvents: Seq[MoveNode]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Seq[StorageNodeId]]] = {
    // Calling get on affectedThing, after filtering out nonEmpty ones, is safe.
    val nodeIds = moveEvents.filter(_.affectedThing.nonEmpty).map(_.affectedThing.get)

    val res = for {
      affectedNodes <- MusitResultT(unitDao.getNodesByIds(mid, nodeIds))
      withParents <- MusitResultT(
                      unitDao.getParentsForNodes(mid, affectedNodes.map(_.nodeId.get))
                    )
      moved <- MusitResultT(
                moveBatch(mid, destination, nodeIds, withParents, moveEvents) {
                  case (to, _, events) =>
                    moveBatchNodes(mid, affectedNodes, to, events)
                }
              )
    } yield moved

    res.value
  }

  /**
   * Moves a batch of objects from their current locations to another node in
   * the storage facility. Note that there are no integrity checks here. The
   * objects are assumed to exist, and will be placed in the node regardless if
   * the exist or not.
   *
   * @param mid         MuseumId
   * @param destination StorageNodeId to place the objects
   * @param moveEvents  A collection of MoveObject events.
   * @param currUsr     the currently authenticated user.
   * @return A MusitResult with a collection of ObjectUUIDs that were moved.
   */
  def moveObjects(
      mid: MuseumId,
      destination: StorageNodeId,
      moveEvents: Seq[MoveObject]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[ObjectUUID]]] = {
    // Calling get on affectedThing, after filtering out nonEmpty ones, is safe.
    val objIdsAndTypes = moveEvents
      .filter(_.affectedThing.nonEmpty)
      .map(a => a.affectedThing.get -> a.objectType)
      .toMap

    val objIds            = objIdsAndTypes.keys.toSeq
    val eventuallyCurrLoc = locObjDao.currentLocations(objIds)

    // format: off
    val res = for {
      currLoc <- MusitResultT(eventuallyCurrLoc)
      movedObjects <- MusitResultT(
        moveBatch(mid, destination, objIds, currLoc, moveEvents) {
          case (_, _, events) =>
            persistMoveEvents(mid, events)(moveDao.batchInsertObjects) { _ =>
              // Again the get on affectedThing is safe since we're guaranteed its
              // presence at this point.
              MusitSuccess(events.map(_.affectedThing.get)) // scalastyle:ignore
            }
        }
      )
    } yield movedObjects
    // format: on

    res.value
  }

  private def toLocHistory(
      mid: MuseumId,
      mo: MoveObject
  )(implicit currUser: AuthenticatedUser) = {
    val fromTuple = findPathAndNamesById(mid, mo.from)
    val toTuple   = findPathAndNamesById(mid, Option(mo.to))

    val lh = for {
      from <- MusitResultT(fromTuple)
      to   <- MusitResultT(toTuple)
    } yield {
      LocationHistory.fromMoveObject(
        moveObject = mo,
        from = FacilityLocation.fromTuple(from),
        to = FacilityLocation.fromTuple(to)
      )
    }
    lh.value
  }

  def objectLocationHistory(
      mid: MuseumId,
      oid: ObjectUUID,
      limit: Option[Int]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[LocationHistory]]] = {
    moveDao.listForObject(mid, oid, limit).flatMap {
      case MusitSuccess(events) =>
        events
          .foldLeft(Future.successful(List.empty[LocationHistory])) { (lhl, e) =>
            toLocHistory(mid, e).flatMap(_.map(lh => lhl.map(_ :+ lh)).getOrElse(lhl))
          }
          .map(MusitSuccess.apply)

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def currentObjectLocation(
      mid: MuseumId,
      oid: ObjectUUID,
      tpe: ObjectType
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[StorageNode]]] = {
    locObjDao.currentLocation(oid, tpe).flatMap {
      case MusitSuccess(maybeNodeId) =>
        maybeNodeId.map(id => getNodeById(mid, id)).getOrElse {
          Future.successful(MusitSuccess(None))
        }

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def currentObjectLocations(
      mid: MuseumId,
      oids: Seq[ObjectUUID]
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Seq[ObjectsLocation]]] = {

    def buildLocationPaths(
        objNodeMap: Map[ObjectUUID, Option[StorageNodeId]],
        nodes: Seq[GenericStorageNode]
    ): Future[MusitResult[Seq[ObjectsLocation]]] = {
      val empty = Future.successful(List.empty[Future[ObjectsLocation]])

      nodes
        .foldLeft(empty) { (objLocs, node) =>
          unitDao.namesForPath(node.path).flatMap {
            case MusitSuccess(pathElems) =>
              val objs = objNodeMap.filter(_._2 == node.id).keys.toSeq
              // Copy node and assign the path to it
              objLocs.map { loc =>
                val ol = ObjectsLocation(node.copy(pathNames = Option(pathElems)), objs)
                loc ::: Future.successful(ol) :: Nil
              }

            case _ => objLocs
          }
        }
        .flatMap(fl => Future.sequence(fl))
        .map(MusitSuccess.apply)
    }

    locObjDao.currentLocations(oids).flatMap {
      case MusitSuccess(objNodeMap) =>
        val nodeIds = objNodeMap.values.flatten.toSeq.distinct

        val res = for {
          nodes  <- MusitResultT(unitDao.getNodesByIds(mid, nodeIds))
          objLoc <- MusitResultT(buildLocationPaths(objNodeMap, nodes))
        } yield objLoc

        res.value

      case err: MusitError =>
        Future.successful(err)
    }
  }

  /**
   *
   * @param oldObjectId
   * @param oldSchemaName
   * @return
   */
  def currNodeForOldObject(
      oldObjectId: Long,
      oldSchemaName: String
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[(StorageNodeId, String)]]] = {
    // Look up object using it's old object ID and the old DB schema name.
    objDao.findByOldId(oldObjectId, oldSchemaName).flatMap {
      case MusitSuccess(mobj) =>
        val res = for {
          obj <- mobj
          oid <- obj.uuid
        } yield {
          MusitResultT(unitDao.currentLocation(obj.museumId, oid)).flatMap {
            case Some(sn) =>
              MusitResultT(unitDao.namesForPath(sn._2)).map { np =>
                // Only authorized users are allowed to see the full path
                // TODO: We probably need to verify the _group_ and not the museum.
                if (currUsr.isAuthorized(obj.museumId)) {
                  Option((sn._1, np.map(_.name).mkString(", ")))
                } else {
                  Option((sn._1, Museum.museumIdToString(obj.museumId)))
                }
              }

            case None =>
              MusitResultT(
                Future.successful[MusitResult[Option[(StorageNodeId, String)]]](
                  MusitSuccess(None)
                )
              )
          }.value
        }
        res.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def nodesOutsideMuseum(
      museumId: MuseumId
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[(StorageNodeDatabaseId, String)]]] = {
    unitDao.findRootLoanNodes(museumId).flatMap {
      case MusitSuccess(rids) =>
        logger.debug(s"Found ${rids.size} external Root nodes: ${rids.mkString(", ")}")
        if (rids.nonEmpty) unitDao.listAllChildrenFor(museumId, rids)
        else Future.successful(MusitSuccess(Seq.empty))
      case err: MusitError => Future.successful(err)
    }
  }

}
