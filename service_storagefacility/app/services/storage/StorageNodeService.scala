package services.storage

import com.google.inject.Inject
import models.storage.{FacilityLocation, LocationHistory}
import models.storage.event.move.{MoveEvent, MoveNode, MoveObject}
import models.storage.nodes._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.LocalObjectDao
import repositories.storage.dao.events.MoveDao
import repositories.storage.dao.nodes.{
  BuildingDao,
  OrganisationDao,
  RoomDao,
  StorageUnitDao
}

import scala.concurrent.Future

class StorageNodeService @Inject()(
    val unitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val orgDao: OrganisationDao,
    val moveDao: MoveDao,
    val locObjDao: LocalObjectDao,
    val envReqService: EnvironmentRequirementService
) extends NodeService {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * Simple check to see if a node with the given exists in a museum.
   *
   * @param mid
   * @param id
   * @return
   */
  def exists(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Boolean]] = {
    unitDao.exists(mid, id)
  }

  def searchByName(
      mid: MuseumId,
      searchStr: String,
      page: Int,
      limit: Int
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
    val theRoot = root.setUpdated(
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

  def addStorageUnit(
      mid: MuseumId,
      storageUnit: StorageUnit
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[StorageUnit]]] = {
    addNode[StorageUnit](
      mid = mid,
      node = storageUnit.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = unitDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => unitDao.setPath(id, path),
      getNode = getStorageUnitById
    )
  }

  def addRoom(
      mid: MuseumId,
      room: Room
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Room]]] = {
    addNode[Room](
      mid = mid,
      node = room.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = roomDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => roomDao.setPath(id, path),
      getNode = getRoomById
    )
  }

  def addBuilding(
      mid: MuseumId,
      building: Building
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Building]]] = {
    addNode[Building](
      mid = mid,
      node = building.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = buildingDao.insert,
      setEnvReq = (node, maybeEnvReq) => node.copy(environmentRequirement = maybeEnvReq),
      updateWithPath = (id, path) => buildingDao.setPath(id, path),
      getNode = getBuildingById
    )
  }

  def addOrganisation(
      mid: MuseumId,
      organisation: Organisation
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Organisation]]] = {
    addNode[Organisation](
      mid = mid,
      node = organisation.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = orgDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => orgDao.setPath(id, path),
      getNode = getOrganisationById
    )
  }

  def updateStorageUnit(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      storageUnit: StorageUnit
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[StorageUnit]]] = {
    val su = storageUnit.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    unitDao.update(mid, id, su).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { _ =>
          for {
            _ <- su.environmentRequirement
                  .map(er => saveEnvReq(mid, id, er))
                  .getOrElse(Future.successful(None))
            node <- getStorageUnitById(mid, id)
          } yield node
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def updateRoom(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      room: Room
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Room]]] = {
    val updateRoom = room.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    roomDao.update(mid, id, updateRoom).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { _ =>
          for {
            _ <- updateRoom.environmentRequirement
                  .map(er => saveEnvReq(mid, id, er))
                  .getOrElse(Future.successful(None))
            node <- getRoomById(mid, id)
          } yield {
            node
          }
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def updateBuilding(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      building: Building
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Building]]] = {
    val updateBuilding = building.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    buildingDao.update(mid, id, updateBuilding).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { _ =>
          for {
            _ <- updateBuilding.environmentRequirement
                  .map(er => saveEnvReq(mid, id, er))
                  .getOrElse(Future.successful(None))
            node <- getBuildingById(mid, id)
          } yield {
            node
          }
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def updateOrganisation(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      organisation: Organisation
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Organisation]]] = {
    val updateOrg = organisation.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    orgDao.update(mid, id, updateOrg).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { _ =>
          for {
            _ <- updateOrg.environmentRequirement
                  .map(er => saveEnvReq(mid, id, er))
                  .getOrElse(Future.successful(None))
            node <- getOrganisationById(mid, id)
          } yield {
            node
          }
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def getStorageUnitById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val eventuallyUnit = unitDao.getByDatabaseId(mid, id)
    getNode(mid, eventuallyUnit) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def getRoomById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Room]]] = {
    val eventuallyRoom = roomDao.getById(mid, id)
    getNode(mid, eventuallyRoom) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def getBuildingById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Building]]] = {
    val eventuallyBuilding = buildingDao.getById(mid, id)
    getNode(mid, eventuallyBuilding) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def getOrganisationById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Organisation]]] = {
    val eventuallyOrg = orgDao.getById(mid, id)
    getNode(mid, eventuallyOrg) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def disambiguateAndGet(
      mid: MuseumId,
      maybeIdType: Option[(StorageNodeDatabaseId, StorageType)]
  ): Future[MusitResult[Option[StorageNode]]] = {
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
  ): Future[MusitResult[Option[StorageNode]]] = {
    logger.debug(s"Disambiguating StorageType $maybeType")
    maybeType.map {
      case StorageType.RootType =>
        unitDao.findRootNode(id)

      case StorageType.RootLoanType =>
        unitDao.findRootNode(id)

      case StorageType.OrganisationType =>
        getOrganisationById(mid, id)

      case StorageType.BuildingType =>
        getBuildingById(mid, id)

      case StorageType.RoomType =>
        getRoomById(mid, id)

      case StorageType.StorageUnitType =>
        getStorageUnitById(mid, id)

    }.getOrElse {
      logger.warn(s"Could not resolve StorageType $maybeType")
      Future.successful(MusitSuccess(None))
    }
  }

  def getNodeByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageNode]]] = {
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
  ): Future[MusitResult[Option[StorageNode]]] = {
    unitDao.getStorageTypeFor(mid, uuid).flatMap { res =>
      res.map(maybeIdType => disambiguateAndGet(mid, maybeIdType)).getOrElse {
        logger.debug(s"Node with UUID $uuid not found")
        Future.successful(MusitSuccess(None))
      }
    }
  }

  def getNodeByOldBarcode(
      mid: MuseumId,
      oldBarcode: Long
  ): Future[MusitResult[Option[StorageNode]]] = {
    (for {
      tuple <- MusitResultT(unitDao.getStorageTypeFor(mid, oldBarcode))
      node <- tuple.map(t => MusitResultT(getNodeByDatabaseId(mid, t._1))).getOrElse {
               MusitResultT(
                 Future.successful[MusitResult[Option[StorageNode]]](MusitSuccess(None))
               )
             }
    } yield node).value
  }

  def rootNodes(mid: MuseumId): Future[MusitResult[Seq[RootNode]]] = {
    (for {
      rootNodes <- MusitResultT(unitDao.findRootNodes(mid))
      sortedNodes = rootNodes.sortBy(_.storageType.displayOrder)
    } yield sortedNodes).value
  }

  def getChildren(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      page: Int,
      limit: Int
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
      id: StorageNodeDatabaseId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Int]]] = {
    unitDao.getByDatabaseId(mid, id).map(_.getOrElse(None)).flatMap {
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
  ): Future[MusitResult[Seq[ID]]] = {
    batchInsert(mid, events).map(_.flatMap(ids => f(ids)))
  }

  private def moveBatchNodes(
      mid: MuseumId,
      affectedNodes: Seq[GenericStorageNode],
      to: GenericStorageNode,
      events: Seq[MoveNode]
  ): Future[MusitResult[Seq[StorageNodeId]]] = {
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

  private def toLocHistory(mid: MuseumId, mo: MoveObject) = {
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
  ): Future[MusitResult[Seq[LocationHistory]]] = {
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
  ): Future[MusitResult[Option[StorageNode]]] = {
    locObjDao.currentLocation(oid, tpe).flatMap {
      case MusitSuccess(maybeNodeId) =>
        maybeNodeId.map(id => getNodeById(mid, id)).getOrElse {
          Future.successful(MusitSuccess(None))
        }

      case err: MusitError =>
        Future.successful(err)
    }
  }

}
