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
package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.event.{EventDao, LocalObjectDao}
import no.uio.musit.microservice.storagefacility.dao.storage._
import no.uio.musit.microservice.storagefacility.domain._
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.EventId
import no.uio.musit.microservice.storagefacility.domain.event.dto.DtoConverters
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveEvent, MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.storage.StorageType.{BuildingType, OrganisationType, RoomType, RootType, StorageUnitType}
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * TODO: Document me!!!
 */
// scalastyle:off number.of.methods
class StorageNodeService @Inject() (
    val unitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val orgDao: OrganisationDao,
    val envReqService: EnvironmentRequirementService,
    val eventDao: EventDao,
    val localObjectDao: LocalObjectDao,
    val statsDao: StorageStatsDao
) {

  val logger = Logger(classOf[StorageNodeService])

  private[service] def saveEnvReq(
    mid: MuseumId,
    nodeId: StorageNodeId,
    envReq: EnvironmentRequirement
  )(implicit currUsr: String): Future[Option[EnvironmentRequirement]] = {
    unitDao.getById(mid, nodeId).flatMap { mayBeNode =>
      mayBeNode.map { _ =>
        val now = dateTimeNow
        val er = EnvRequirement.toEnvRequirementEvent(nodeId, now, envReq)

        envReqService.add(mid, er).map {
          case MusitSuccess(success) =>
            logger.debug("Successfully wrote environment requirement data " +
              s"for node $nodeId")
            Some(EnvRequirement.fromEnvRequirementEvent(er))

          case err: MusitError =>
            logger.error("Something went wrong while storing the environment " +
              s"requirements for node $nodeId")
            None
        }
      }.getOrElse(Future.successful(None))
    }
  }

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

  def addRoot(mid: MuseumId): Future[MusitResult[Option[Root]]] = {
    unitDao.insertRoot(mid, Root()).flatMap { nodeId =>
      val path = NodePath.empty.appendChild(nodeId)
      unitDao.setRootPath(nodeId, path).flatMap {
        case MusitSuccess(()) =>
          logger.debug(s"Updated root path...looking up node with ID $nodeId")
          unitDao.findRootNode(nodeId)

        case err: MusitError =>
          Future.successful(err)
      }
    }
  }

  /**
   * Find the NodePath for the given storageNodeId.
   */
  private[service] def findPath(id: Option[StorageNodeId]): Future[Option[NodePath]] = {
    id.map(unitDao.getPathById).getOrElse(Future.successful(None))
  }

  def isValidPosition[T <: StorageNode](
    mid: MuseumId,
    node: T,
    dest: NodePath
  ): Future[Boolean] = {

    if (!dest.childOf(node.path)) {

      val maybeDestId = dest.asIdSeq.lastOption
      // Get the StorageType for the elements in the destination path so we can
      // use it to verify that nodes are placed on a valid location
      unitDao.getStorageTypesInPath(mid, dest).map { idTypeTuples =>
        // Identify the type of node we want to place in the hierarchy, and
        // validate if the destination location is valid for the given type.
        node.storageType match {
          case RootType => Root.isValidLocation(dest)
          case OrganisationType => Organisation.isValidLocation(maybeDestId, idTypeTuples)
          case BuildingType => Building.isValidLocation(maybeDestId, idTypeTuples)
          case RoomType => Room.isValidLocation(maybeDestId, idTypeTuples)
          case StorageUnitType => StorageUnit.isValidLocation(maybeDestId, idTypeTuples)
        }
      }
    } else {
      Future.successful(false)
    }
  }

  // A couple of type aliases to reduce the length of some function args.
  type NodeInsertIO[A] = (MuseumId, A) => Future[StorageNodeId]
  type SetEnvReq[A] = (A, Option[EnvironmentRequirement]) => A
  type NodeUpdateIO[A] = (StorageNodeId, NodePath) => Future[MusitResult[Unit]]
  type GetNodeIO[A] = (MuseumId, StorageNodeId) => Future[MusitResult[Option[A]]]

  /**
   * Helper function that wraps the process of inserting a new StorageNode.
   *
   * @param node
   * @param insert
   * @param setEnvReq
   * @param updateWithPath
   * @param currUsr
   * @tparam T
   * @return
   */
  private def addNode[T <: StorageNode](
    mid: MuseumId,
    node: T,
    insert: NodeInsertIO[T],
    setEnvReq: SetEnvReq[T],
    updateWithPath: NodeUpdateIO[T],
    getNode: GetNodeIO[T]
  )(implicit currUsr: String): Future[MusitResult[Option[T]]] = {
    for {
      maybePath <- findPath(node.isPartOf)
      // Call te insert function to persist the node.
      nodeId <- insert(mid, node)
      pathUpdated <- updateWithPath(nodeId, maybePath.getOrElse(NodePath.empty).appendChild(nodeId))
      _ <- node.environmentRequirement.map(er => saveEnvReq(mid, nodeId, er)).getOrElse(Future.successful(None))
      theNode <- getNode(mid, nodeId)
    } yield {
      theNode
    }
  }

  /**
   * TODO: Document me!
   */
  def addStorageUnit(
    mid: MuseumId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[MusitResult[Option[StorageUnit]]] = {
    addNode[StorageUnit](
      mid = mid,
      node = storageUnit,
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
  )(implicit currUsr: String): Future[MusitResult[Option[Room]]] = {
    addNode[Room](
      mid = mid,
      node = room,
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
  )(implicit currUsr: String): Future[MusitResult[Option[Building]]] = {
    addNode[Building](
      mid = mid,
      node = building,
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
  )(implicit currUsr: String): Future[MusitResult[Option[Organisation]]] = {
    addNode[Organisation](
      mid = mid,
      node = organisation,
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
    id: StorageNodeId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[MusitResult[Option[StorageUnit]]] = {
    unitDao.update(mid, id, storageUnit).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { numUpdated =>
          for {
            _ <- storageUnit.environmentRequirement.map(er => saveEnvReq(mid, id, er))
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
    id: StorageNodeId,
    room: Room
  )(implicit currUsr: String): Future[MusitResult[Option[Room]]] = {
    roomDao.update(mid, id, room).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { numUpdated =>
          for {
            _ <- room.environmentRequirement.map(er => saveEnvReq(mid, id, er))
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
    id: StorageNodeId,
    building: Building
  )(implicit currUsr: String): Future[MusitResult[Option[Building]]] = {
    buildingDao.update(mid, id, building).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { numUpdated =>
          for {
            _ <- building.environmentRequirement.map(er => saveEnvReq(mid, id, er))
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
    id: StorageNodeId,
    organisation: Organisation
  )(implicit currUsr: String): Future[MusitResult[Option[Organisation]]] = {
    orgDao.update(mid, id, organisation).flatMap {
      case MusitSuccess(maybeRes) =>
        maybeRes.map { numUpdated =>
          for {
            _ <- organisation.environmentRequirement.map(er => saveEnvReq(mid, id, er))
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

  type CopyNode[A <: StorageNode] = (A, Option[EnvironmentRequirement], Option[Seq[NamedPathElement]]) => A

  /**
   * Helper function that applies the common logic for fetching a storage node.
   *
   * @param mid
   * @param id
   * @param eventuallyMaybeNode
   * @param cp
   * @tparam A
   * @return
   */
  private def nodeById[A <: StorageNode](
    mid: MuseumId,
    id: StorageNodeId,
    eventuallyMaybeNode: Future[Option[A]]
  )(cp: CopyNode[A]): Future[MusitResult[Option[A]]] = {
    val eventuallyMaybeEnvReq = getEnvReq(mid, id)
    for {
      maybeNode <- eventuallyMaybeNode
      maybeEnvReq <- eventuallyMaybeEnvReq
      namedPathElems <- maybeNode.map { node =>
        unitDao.namesForPath(node.path)
      }.getOrElse(Future.successful(Seq.empty))
    } yield {
      val maybePathElems = {
        if (namedPathElems.nonEmpty) Some(namedPathElems)
        else None
      }
      MusitSuccess(maybeNode.map(n => cp(n, maybeEnvReq, maybePathElems)))
    }
  }

  /**
   * TODO: Document me!
   */
  def getStorageUnitById(
    mid: MuseumId,
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val eventuallyUnit = unitDao.getById(mid, id)
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
    id: StorageNodeId
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
    id: StorageNodeId
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
    id: StorageNodeId
  ): Future[MusitResult[Option[Organisation]]] = {
    val eventuallyOrg = orgDao.getById(mid, id)
    nodeById(mid, id, eventuallyOrg) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  private def getEnvReq(
    mid: MuseumId,
    id: StorageNodeId
  ): Future[Option[EnvironmentRequirement]] = {
    envReqService.findLatestForNodeId(mid, id).map {
      case MusitSuccess(maybeEnvRequirement) => maybeEnvRequirement
      case _ => None

    }.recover {
      case NonFatal(ex) =>
        // If we fail fetching the envreq event, we'll return None.
        logger.warn("Something went wrong trying to locate latest " +
          s"environment requirement data for unit $id", ex)
        None
    }
  }

  /**
   * TODO: Document me!
   */
  def getNodeById(
    mid: MuseumId,
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageNode]]] = {
    unitDao.getStorageType(id).flatMap { res =>
      res.map { maybeType =>
        logger.debug(s"Disambiguating StorageType $maybeType")

        maybeType.map {
          case StorageType.RootType =>
            logger.warn(s"Trying to read root node $id in getNodeById.")
            Future.successful(MusitSuccess(None))

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

  /**
   * TODO: Document me!
   */
  def rootNodes(mid: MuseumId): Future[Seq[StorageNode]] = unitDao.findRootNodes(mid)

  /**
   * TODO: Document me!
   */
  def getChildren(mid: MuseumId, id: StorageNodeId): Future[Seq[GenericStorageNode]] = {
    unitDao.getChildren(mid, id)
  }

  /**
   * TODO: Document me!
   */
  def getStorageType(id: StorageNodeId): Future[MusitResult[Option[StorageType]]] = {
    unitDao.getStorageType(id)
  }

  /**
   * TODO: Document me!
   */
  def verifyStorageTypeMatchesDatabase(
    id: StorageNodeId,
    expected: StorageType
  ): Future[Boolean] = {
    getStorageType(id).map {
      case MusitSuccess(correct) if Option(expected) == correct =>
        true

      case MusitSuccess(wrong) =>
        logger.warn(
          s"StorageUnit with id: $id was expected to have storage " +
            s"type: ${expected.entryName}, but had the " +
            s"type: ${wrong.map(_.entryName).getOrElse("NA")} in the database."
        )
        false

      case err: MusitError =>
        false
    }
  }

  def nodeStats(
    mid: MuseumId,
    nodeId: StorageNodeId
  ): Future[MusitResult[Option[NodeStats]]] = {
    getNodeById(mid, nodeId).flatMap {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { node =>
          val eventuallyTotal = statsDao.totalObjectCount(node.path)
          val eventuallyDirect = statsDao.directObjectCount(nodeId)
          val eventuallyNodeCount = statsDao.childCount(nodeId)

          for {
            total <- eventuallyTotal
            direct <- eventuallyDirect
            nodeCount <- eventuallyNodeCount
          } yield {
            MusitSuccess(Some(NodeStats(nodeCount, direct, total)))
          }
        }.getOrElse {
          Future.successful(MusitSuccess(None))
        }

      case err: MusitError =>
        Future.successful(err)
    }
  }

  private def isEmpty(node: StorageNode): Future[Boolean] = {
    node.id.map { nodeId =>
      val eventuallyTotal = statsDao.directObjectCount(nodeId)
      val eventuallyNode = statsDao.childCount(nodeId)

      for {
        total <- eventuallyTotal
        nodeCount <- eventuallyNode
      } yield (total + nodeCount) == 0
    }.getOrElse(Future.successful(false))
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
    id: StorageNodeId
  )(implicit currUsr: String): Future[MusitResult[Option[Int]]] = {
    unitDao.getById(mid, id).flatMap {
      case Some(node) =>
        isEmpty(node).flatMap { empty =>
          if (empty) unitDao.markAsDeleted(mid, id).map(_.map(Some.apply))
          else Future.successful(MusitSuccess(Some(-1)))
        }

      case None =>
        Future.successful(MusitSuccess(None))
    }
  }

  /**
   * Helper to encapsulate shared logic between the public move methods.
   */
  private def persistMoveEvent[ID <: MusitId](
    mid: MuseumId,
    id: ID,
    event: MoveEvent
  )(f: EventId => Future[MusitResult[EventId]]): Future[MusitResult[EventId]] = {
    val dto = DtoConverters.MoveConverters.moveToDto(event)
    eventDao.insertEvent(mid, dto).flatMap(eventId => f(eventId)).recover {
      case NonFatal(ex) =>
        val msg = s"An exception occured trying to move $id"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  private def move[E <: MoveEvent](
    event: E,
    eventuallyMaybeCurrent: Future[Option[GenericStorageNode]],
    eventuallyMaybeTo: Future[Option[GenericStorageNode]]
  )(
    mv: (GenericStorageNode, GenericStorageNode) => Future[MusitResult[EventId]]
  ): Future[MusitResult[EventId]] = {
    val eventuallyExistence = for {
      maybeCurrent <- eventuallyMaybeCurrent
      maybeTo <- eventuallyMaybeTo
    } yield (maybeCurrent, maybeTo)

    eventuallyExistence.flatMap {
      case (maybeCurrent: Option[GenericStorageNode], maybeTo: Option[GenericStorageNode]) =>
        maybeCurrent.flatMap(current => maybeTo.map(to => mv(current, to)))
          .getOrElse {
            Future.successful(MusitValidationError("Could not find to or from node."))
          }
    }
  }

  /**
   * TODO: This is a mess...refactor me when time allows
   */
  def moveNode(
    mid: MuseumId,
    id: StorageNodeId,
    event: MoveNode
  )(implicit currUsr: String): Future[MusitResult[EventId]] = {
    move(event, unitDao.getNodeById(mid, id), unitDao.getNodeById(mid, event.to)) { (curr, to) =>

      // TODO: evaluate if the to location is valid given the type of the node being moved

      val theEvent = event.copy(from = curr.id)
      logger.debug(s"Going to move node $id from ${curr.path} to ${to.path}")
      unitDao.updatePathForSubTree(id, curr.path, to.path.appendChild(id)).flatMap {
        case MusitSuccess(numUpdated) =>
          persistMoveEvent(mid, id, theEvent) { eventId =>
            unitDao.updatePartOf(id, Some(event.to)).map { updRes =>
              logger.debug(s"Update partOf result $updRes")
              MusitSuccess(eventId)
            }
          }

        case err: MusitError => Future.successful(err)
      }
    }
  }

  /**
   * TODO: Document me!
   */
  def moveObject(
    mid: MuseumId,
    objectId: ObjectId,
    event: MoveObject
  )(implicit currUsr: String): Future[MusitResult[EventId]] = {
    val eventuallyMaybeCurrent = localObjectDao.currentLocation(objectId)
      .flatMap { maybeId =>
        maybeId.map(id => unitDao.getNodeById(mid, id)).getOrElse(Future.successful(None))
      }

    move(event, eventuallyMaybeCurrent, unitDao.getNodeById(mid, event.to)) { (curr, to) =>
      val theEvent = event.copy(from = curr.id)
      logger.debug(s"Going to move object $objectId from ${curr.path} to ${to.path}")
      persistMoveEvent(mid, objectId, theEvent) { eventId =>
        Future.successful(MusitSuccess(eventId))
      }
    }
  }

  /**
   * Helper method to find PathNames for a potentially present StorageNodeId.
   *
   * @param maybeId Option[StorageNodeId]
   * @return Future[(NodePath, Seq[NamedPathElement])]
   */
  private def findPathAndNames(
    maybeId: Option[StorageNodeId]
  ): Future[(NodePath, Seq[NamedPathElement])] = {
    findPath(maybeId).flatMap { maybePath =>
      maybePath.map(p => unitDao.namesForPath(p).map(names => (p, names)))
        .getOrElse(Future.successful((NodePath.empty, Seq.empty)))
    }
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
      Future.sequence {
        events.map { e =>
          val fromTuple = findPathAndNames(e.from)
          val toTuple = findPathAndNames(Option(e.to))

          for {
            from <- fromTuple
            to <- toTuple
          } yield {
            LocationHistory(
              registeredBy = e.registeredBy.getOrElse(""),
              // registered date is required on event, so it must be there.
              registeredDate = e.registeredDate.get,
              doneBy = e.doneBy,
              doneDate = e.doneDate,
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

  def getCurrentObjectLocation(mid: MuseumId, oid: Long): Future[MusitResult[Option[GenericStorageNode]]] = {
    val currentNodeId = localObjectDao.currentLocation(oid)
    val genNode = currentNodeId.flatMap { optCurrentNodeId =>
      optCurrentNodeId.map { id =>
        unitDao.getNodeById(mid, id).map(MusitSuccess.apply)
      }.getOrElse(Future.successful(MusitSuccess(None)))
    }
    genNode
  }
}

// scalastyle:on number.of.methods
