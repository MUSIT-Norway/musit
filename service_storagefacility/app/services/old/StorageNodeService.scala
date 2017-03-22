package services.old

import com.google.inject.Inject
import models.storage.event.dto.DtoConverters
import models.storage.event.old.move.{MoveEvent, MoveNode, MoveObject}
import models.storage.nodes._
import models.storage.{FacilityLocation, LocationHistory, ObjectsLocation}
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.LocalObjectDao
import repositories.storage.dao.nodes.{
  BuildingDao,
  OrganisationDao,
  RoomDao,
  StorageUnitDao
}
import repositories.storage.old_dao.event.EventDao

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject()(
    val unitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val orgDao: OrganisationDao,
    val envReqService: EnvironmentRequirementService,
    val eventDao: EventDao,
    val localObjectDao: LocalObjectDao
) extends NodeService {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * Simple check to see if a node with the given exists in a museum.
   *
   * @param mid
   * @param id
   * @return
   */
  def exists(mid: MuseumId, id: StorageNodeDatabaseId): Future[MusitResult[Boolean]] = {
    unitDao.exists(mid, id)
  }

  /**
   * Adds a Root node to a Museum.
   *
   * @param mid
   * @param root
   * @return
   */
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

  /**
   * TODO: Document me!
   */
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

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!
   */
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
          } yield {
            node
          }
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!!!
   */
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

  /**
   * TODO: Document me!
   */
  def getStorageUnitById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val eventuallyUnit = unitDao.getByDatabaseId(mid, id)
    nodeById(mid, id, eventuallyUnit) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getRoomById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Room]]] = {
    val eventuallyRoom = roomDao.getById(mid, id)
    nodeById(mid, id, eventuallyRoom) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Building]]] = {
    val eventuallyBuilding = buildingDao.getById(mid, id)
    nodeById(mid, id, eventuallyBuilding) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getOrganisationById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Organisation]]] = {
    val eventuallyOrg = orgDao.getById(mid, id)
    nodeById(mid, id, eventuallyOrg) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  /**
   * TODO: Document me!
   */
  def getNodeById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[StorageNode]]] = {
    unitDao.getStorageTypeFor(mid, id).flatMap { res =>
      res.map { maybeType =>
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
      }.getOrElse {
        logger.debug(s"Node $id not found")
        Future.successful(MusitSuccess(None))
      }
    }
  }

  def getNodeByStorageNodeId(
      mid: MuseumId,
      uuid: StorageNodeId
  ): Future[MusitResult[Option[StorageNode]]] = {
    (for {
      tuple <- MusitResultT(unitDao.getStorageTypeFor(mid, uuid))
      node <- tuple.map(t => MusitResultT(getNodeById(mid, t._1))).getOrElse {
               MusitResultT(
                 Future.successful[MusitResult[Option[StorageNode]]](MusitSuccess(None))
               )
             }
    } yield node).value
  }

  def getNodeByOldBarcode(
      mid: MuseumId,
      oldBarcode: Long
  ): Future[MusitResult[Option[StorageNode]]] = {
    (for {
      tuple <- MusitResultT(unitDao.getStorageTypeFor(mid, oldBarcode))
      node <- tuple.map(t => MusitResultT(getNodeById(mid, t._1))).getOrElse {
               MusitResultT(
                 Future.successful[MusitResult[Option[StorageNode]]](MusitSuccess(None))
               )
             }
    } yield node).value
  }

  /**
   * TODO: Document me!
   */
  def rootNodes(mid: MuseumId): Future[MusitResult[Seq[RootNode]]] = {
    (for {
      rootNodes <- MusitResultT(unitDao.findRootNodes(mid))
      sortedNodes = rootNodes.sortBy(_.storageType.displayOrder)
    } yield sortedNodes).value

  }

  /**
   * TODO: Document me!
   */
  def getChildren(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      page: Int,
      limit: Int
  ): Future[MusitResult[PagedResult[GenericStorageNode]]] = {
    unitDao.getChildren(mid, id, page, limit)
  }

  /**
   * TODO: Document me!
   *
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
  private def persistMoveEvents[ID <: MusitId, E <: MoveEvent](
      mid: MuseumId,
      events: Seq[E]
  )(
      f: Seq[EventId] => MusitResult[Seq[ID]]
  ): Future[MusitResult[Seq[ID]]] = {
    val dtos = events.map(DtoConverters.MoveConverters.moveToDto)

    eventDao.insertEvents(mid, dtos).map(ids => f(ids)).recover {
      case NonFatal(ex) =>
        val msg = "An exception occurred registering a batch move with ids: " +
          s" ${events.map(_.id.getOrElse("<empty>")).mkString(", ")}"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  private def moveBatchNodes(
      mid: MuseumId,
      affectedNodes: Seq[GenericStorageNode],
      to: GenericStorageNode,
      curr: CurrLocType[StorageNodeDatabaseId],
      events: Seq[MoveNode]
  ): Future[MusitResult[Seq[StorageNodeDatabaseId]]] = {
    logger.debug(s"Destination node is ${to.id} with path ${to.path}")
    logger.debug(s"Filtering away invalid placement of nodes in ${to.id}.")
    // Filter away nodes that didn't pass first round of validation
    val nodesToMove =
      affectedNodes.filter(n => events.exists(_.affectedThing.contains(n.id.get)))
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
          mvRes <- persistMoveEvents(mid, validEvents)(
                    _ => MusitSuccess(validNodes.flatMap(_.id))
                  ) // scalastyle:ignore
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
      destination: StorageNodeDatabaseId,
      moveEvents: Seq[MoveNode]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Seq[StorageNodeDatabaseId]]] = {
    // Calling get on affectedThing, after filtering out nonEmpty ones, is safe.
    val nodeIds = moveEvents.filter(_.affectedThing.nonEmpty).map(_.affectedThing.get)

    val res = for {
      affectedNodes <- MusitResultT(unitDao.getNodesByDatabaseIds(mid, nodeIds))
      currLoc = affectedNodes.map(n => (n.id.get, n.isPartOf)).toMap
      moved <- MusitResultT(moveBatch(mid, destination, nodeIds, currLoc, moveEvents) {
                case (to, curr, events) =>
                  moveBatchNodes(mid, affectedNodes, to, curr, events)
              })
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
   * @param destination StorageNodeDatabaseId to place the objects
   * @param moveEvents  A collection of MoveObject events.
   * @param currUsr     the currently authenticated user.
   * @return A MusitResult with a collection of ObjectIds that were moved.
   */
  def moveObjects(
      mid: MuseumId,
      destination: StorageNodeDatabaseId,
      moveEvents: Seq[MoveObject]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[ObjectId]]] = {
    // Calling get on affectedThing, after filtering out nonEmpty ones, is safe.
    val objIdsAndTypes = moveEvents
      .filter(_.affectedThing.nonEmpty)
      .map(a => a.affectedThing.get -> a.objectType)
      .toMap

    val objIds     = objIdsAndTypes.keys.toSeq
    val currentLoc = localObjectDao.currentLocations(objIds)

    // format: off
    val res = for {
      currentLoc <- MusitResultT(localObjectDao.currentLocations(objIds))
      movedObjects <- MusitResultT(
        moveBatch(mid, destination, objIds, currentLoc, moveEvents) {
          case (_, _, events) =>
            persistMoveEvents(mid, events) { eventIds =>
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

  /**
   * Returns the
   *
   * @param oid the object ID to fetch history for
   * @return
   */
  def objectLocationHistory(
      mid: MuseumId,
      oid: ObjectId,
      limit: Option[Int]
  ): Future[MusitResult[Seq[LocationHistory]]] = {
    val res = eventDao.getObjectLocationHistory(mid, oid, limit).flatMap { events =>
      events.foldLeft(Future.successful(List.empty[LocationHistory])) { (lhl, e) =>
        val fromTuple = findPathAndNames(mid, e.from)
        val toTuple   = findPathAndNames(mid, Option(e.to))

        val locationHistoryResult = for {
          from <- MusitResultT(fromTuple)
          to   <- MusitResultT(toTuple)
        } yield {
          LocationHistory(
            // registered by and date is required on Event, so they must be there.
            registeredBy = e.registeredBy.get,
            registeredDate = e.registeredDate.get,
            doneBy = e.doneBy,
            doneDate = e.doneDate,
            id = e.affectedThing.get,
            objectType = e.objectType,
            from = FacilityLocation(
              path = from._1,
              pathNames = from._2
            ),
            to = FacilityLocation(
              path = to._1,
              pathNames = to._2
            )
          )
        }

        locationHistoryResult.value.flatMap {
          case MusitSuccess(lh) => lhl.map(_ :+ lh)
          case _: MusitError    => lhl
        }
      }
    }
    res.map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"Fetching of location history for object $oid failed"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  /**
   *
   * @param mid
   * @param oid
   * @return
   */
  def currentObjectLocation(
      mid: MuseumId,
      oid: ObjectId,
      tpe: ObjectType
  ): Future[MusitResult[Option[StorageNode]]] = {
    val currentNodeId = localObjectDao.currentLocation(oid, tpe)
    currentNodeId.flatMap { optCurrentNodeId =>
      optCurrentNodeId.map { id =>
        getNodeById(mid, id)
      }.getOrElse(Future.successful(MusitSuccess(None)))
    }
  }

  /**
   *
   * @param mid
   * @param oids
   * @return
   */
  def currentObjectLocations(
      mid: MuseumId,
      oids: Seq[ObjectId]
  ): Future[MusitResult[Seq[ObjectsLocation]]] = {

    def findObjectLocations(
        objNodeMap: Map[ObjectId, Option[StorageNodeDatabaseId]],
        nodes: Seq[GenericStorageNode]
    ): Future[MusitResult[Seq[ObjectsLocation]]] = {
      nodes
        .foldLeft(Future.successful(List.empty[Future[ObjectsLocation]])) {
          case (ols, node) =>
            unitDao.namesForPath(node.path).flatMap {
              case MusitSuccess(namedPaths) =>
                val objects = objNodeMap.filter(_._2 == node.id).keys.toSeq
                // Copy node and set path to it
                ols.map { objLoc =>
                  objLoc :+ Future.successful(
                    ObjectsLocation(node.copy(pathNames = Option(namedPaths)), objects)
                  )
                }

              case _ => ols
            }

        }
        .flatMap(fl => Future.sequence(fl))
        .map(MusitSuccess.apply)
    }

    localObjectDao.currentLocations(oids).flatMap {
      case MusitSuccess(objNodeMap) =>
        val nodeIds = objNodeMap.values.flatten.toSeq.distinct

        val res = for {
          nodes  <- MusitResultT(unitDao.getNodesByDatabaseIds(mid, nodeIds))
          objLoc <- MusitResultT(findObjectLocations(objNodeMap, nodes))
        } yield objLoc
        res.value

      case err: MusitError => Future.successful(err)
    }
  }

  /**
   *
   * @param mid
   * @param searchStr
   * @param page
   * @param limit
   * @return
   */
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
          s"Too few letters in search string $searchStr storageNode name."
        )
      }
    }
  }

}
