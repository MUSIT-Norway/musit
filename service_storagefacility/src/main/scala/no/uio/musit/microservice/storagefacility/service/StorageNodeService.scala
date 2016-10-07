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
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.dto.{BaseEventDto, DtoConverters}
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.{MuseumId, NodePath, NodeStats}
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject() (
    val unitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val organisationDao: OrganisationDao,
    val envReqService: EnvironmentRequirementService,
    val eventDao: EventDao,
    val localObjectDao: LocalObjectDao,
    val statsDao: StorageStatsDao,
    val storageNodeDao: StorageUnitDao
) {

  val logger = Logger(classOf[StorageNodeService])

  private[service] def saveEnvReq(
    mid: MuseumId,
    nodeId: StorageNodeId,
    envReq: EnvironmentRequirement
  )(implicit currUsr: String): Future[Option[EnvironmentRequirement]] = {
    storageNodeDao.getById(mid, nodeId).flatMap {
      mayBeNode =>
        mayBeNode.fold(Future.successful(None: Option[EnvironmentRequirement])) { _ =>
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
        }
    }
  }

  def addRoot(mid: MuseumId, root: Root): Future[Root] = {
    unitDao.insertRoot(mid, root).flatMap { r =>
      val id = r.id.get
      val path = r.path.getOrElse(NodePath.empty).appendChild(id)
      unitDao.updateRootPath(id, path).map { mr =>
        logger.debug(s"Updated root path and go back $mr")
        mr.getOrElse(r)
      }
    }
  }

  /**
   * Find the NodePath for the given storageNodeId.
   */
  private[service] def findPath(
    id: Option[StorageNodeId]
  ): Future[Option[NodePath]] = {
    id.map(unitDao.getPathById).getOrElse {
      Future.successful(None)
    }
  }

  // A couple of type aliases to reduce the length of some function args.
  type NodeInsertIO[A] = (MuseumId, A) => Future[A]
  type SetEnvReq[A] = (A, Option[EnvironmentRequirement]) => A
  type NodeUpdateIO[A] = (StorageNodeId, A, NodePath) => Future[A]

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
    updateWithPath: NodeUpdateIO[T]
  )(implicit currUsr: String): Future[T] = {
    for {
      maybePath <- findPath(node.isPartOf)
      // Call te insert function to persist the node.
      addedNode <- insert(mid, node).flatMap { added =>
        logger.debug(s"${node.getClass.getSimpleName} was added with id ${added.id}")
        val maybeWithEnvReq = for {
          nodeId <- added.id
          envReq <- node.environmentRequirement
        } yield {
          logger.debug(s"Saving new environment requirement data " +
            s"for ${node.getClass.getSimpleName} with id $nodeId")
          saveEnvReq(mid, nodeId, envReq).map { maybeEnvReq =>
            setEnvReq(added, maybeEnvReq)
          }
        }
        maybeWithEnvReq.getOrElse(Future.successful(added))
      }
      withPath <- {
        // We can get on the ID here because we know it's present. Otherwise the for
        // comprehension would've been aborted already with a failed Future.
        val id = addedNode.id.get
        logger.debug(s"Updating node $id with correct path")
        updateWithPath(
          id,
          addedNode,
          maybePath.getOrElse(NodePath.empty).appendChild(id)
        )
      }
    } yield withPath
  }

  /**
   * TODO: Document me!
   */
  def addStorageUnit(
    mid: MuseumId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[StorageUnit] = {
    addNode[StorageUnit](
      mid = mid,
      node = storageUnit,
      insert = unitDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, created, path) =>
      unitDao.setPath(id, path).map {
        case MusitSuccess(()) => created.copy(path = Some(path))
        case err: MusitError => created
      }
    )
  }

  /**
   * TODO: Document me!!!
   */
  def addRoom(mid: MuseumId, room: Room)(implicit currUsr: String): Future[Room] = {
    addNode[Room](
      mid = mid,
      node = room,
      insert = roomDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = { (id, created, path) =>
      logger.debug(s"")
      roomDao.setPath(id, path).map {
        case MusitSuccess(()) => created.copy(path = Some(path))
        case err: MusitError => created
      }
    }
    )
  }

  /**
   * TODO: Document me!!!
   */
  def addBuilding(mid: MuseumId, building: Building)(implicit currUsr: String): Future[Building] = {
    addNode[Building](
      mid = mid,
      node = building,
      insert = buildingDao.insert,
      setEnvReq = (node, maybeEnvReq) => node.copy(environmentRequirement = maybeEnvReq),
      updateWithPath = (id, created, path) =>
      buildingDao.setPath(id, path).map {
        case MusitSuccess(()) => created.copy(path = Some(path))
        case err: MusitError => created
      }
    )
  }

  /**
   * TODO: Document me!!!
   */
  def addOrganisation(mid: MuseumId, organisation: Organisation)(implicit currUsr: String): Future[Organisation] = {
    addNode[Organisation](
      mid = mid,
      node = organisation,
      insert = organisationDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, created, path) =>
      organisationDao.setPath(id, path).map {
        case MusitSuccess(()) => created.copy(path = Some(path))
        case err: MusitError => created
      }
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
    unitDao.update(mid, id, storageUnit).flatMap { maybeUnit =>
      logger.debug(s"Successfully updated storage unit $id")
      val maybeWithEnvReq = for {
        su <- maybeUnit
        envReq <- storageUnit.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for unit node $id")
        saveEnvReq(mid, id, envReq).map { er =>
          Some(su.copy(environmentRequirement = er))
        }
      }
      maybeWithEnvReq.map(_.map(mu => MusitSuccess(mu))).getOrElse {
        Future.successful(MusitSuccess(maybeUnit))
      }
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
    roomDao.update(mid, id, room).flatMap { maybeRoom =>
      logger.debug(s"Successfully updated storage room $id")
      val maybeWithEnvReq = for {
        r <- maybeRoom
        envReq <- room.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for room node $id")
        saveEnvReq(mid, id, envReq).map { er =>
          Some(r.copy(environmentRequirement = er))
        }
      }
      maybeWithEnvReq.map(_.map(mr => MusitSuccess(mr))).getOrElse {
        Future.successful(MusitSuccess(maybeRoom))
      }
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
    buildingDao.update(mid, id, building).flatMap { maybeBuilding =>
      logger.debug(s"Successfully updated storage building $id")
      val maybeWithEnvReq = for {
        b <- maybeBuilding
        envReq <- building.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for building node $id")
        saveEnvReq(mid, id, envReq).map { er =>
          Some(b.copy(environmentRequirement = er))
        }
      }
      maybeWithEnvReq.map(_.map(mb => MusitSuccess(mb))).getOrElse {
        Future.successful(MusitSuccess(maybeBuilding))
      }
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
    organisationDao.update(mid, id, organisation).flatMap { maybeOrg =>
      logger.debug(s"Successfully updated storage building $id")
      val maybeWithEnvReq = for {
        org <- maybeOrg
        envReq <- organisation.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for organisation node $id")
        saveEnvReq(mid, id, envReq).map { er =>
          Some(org.copy(environmentRequirement = er))
        }
      }
      maybeWithEnvReq.map(_.map(mo => MusitSuccess(mo))).getOrElse {
        Future.successful(MusitSuccess(maybeOrg))
      }
    }
  }

  /**
   * TODO: Document me!
   */
  def getStorageUnitById(
    mid: MuseumId,
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    for {
      unitRes <- unitDao.getById(mid, id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(mid, id)
    } yield {
      unitRes.map { maybeUnit =>
        maybeUnit.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getRoomById(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Option[Room]]] = {
    for {
      roomRes <- roomDao.getById(mid, id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(mid, id)
    } yield {
      roomRes.map { maybeRoom =>
        maybeRoom.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Option[Building]]] = {
    for {
      buildingRes <- buildingDao.getById(mid, id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(mid, id)
    } yield {
      buildingRes.map { maybeBuilding =>
        maybeBuilding.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }

  }

  /**
   * TODO: Document me!!!
   */
  def getOrganisationById(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Option[Organisation]]] = {
    for {
      orgRes <- organisationDao.getById(mid, id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(mid, id)
    } yield {
      orgRes.map { maybeOrg =>
        maybeOrg.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  private def getEnvReq(mid: MuseumId, id: StorageNodeId): Future[Option[EnvironmentRequirement]] = {
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

  def nodeStats(mid: MuseumId, nodeId: StorageNodeId): Future[MusitResult[Option[NodeStats]]] = {
    getNodeById(mid, nodeId).flatMap {
      case MusitSuccess(maybeNode) =>
        maybeNode.flatMap { node =>
          node.path.map { nodePath =>
            val eventuallyTotal = Future.successful(0) // statsDao.totalObjectCount(nodePath)
            val eventuallyDirect = statsDao.directObjectCount(nodeId)
            val eventuallyNodeCount = statsDao.childCount(nodeId)

            for {
              total <- eventuallyTotal
              direct <- eventuallyDirect
              nodeCount <- eventuallyNodeCount
            } yield {
              MusitSuccess(Some(NodeStats(nodeCount, direct, total)))
            }
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
  def deleteNode(mid: MuseumId, id: StorageNodeId)(implicit currUsr: String): Future[MusitResult[Option[Int]]] = {
    getNodeById(mid, id).flatMap {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { node =>
          isEmpty(node).flatMap { empty =>
            if (empty) unitDao.markAsDeleted(mid, id).map(_.map(Some.apply))
            else Future.successful(MusitSuccess(Some(-1)))
          }
        }.getOrElse(Future.successful(MusitSuccess(None)))

      case error: MusitError =>
        Future.successful(error)
    }
  }

  /**
   * Helper to encapsulate shared logic between the public move methods.
   */
  private def move(
    mid: MuseumId,
    id: Long,
    dto: BaseEventDto
  )(f: Long => Future[MusitResult[Long]]): Future[MusitResult[Long]] = {
    eventDao.insertEvent(mid, dto).flatMap(eventId => f(eventId)).recover {
      case NonFatal(ex) =>
        val msg = s"An exception occured trying to move $id"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  /**
   * TODO: This is a mess...refactor me when time allows
   */
  def moveNode(
    mid: MuseumId,
    id: StorageNodeId,
    event: MoveNode
  )(implicit currUsr: String): Future[MusitResult[Long]] = {
    val dto = DtoConverters.MoveConverters.moveNodeToDto(event)

    def mv(fromPath: NodePath, toPath: NodePath): Future[MusitResult[Long]] = {
      unitDao.updatePathForSubTree(id, fromPath, toPath.appendChild(id)).flatMap {
        case MusitSuccess(numUpdated) =>
          move(mid, id, dto) { eventId =>
            unitDao.updatePartOf(id, Some(event.to.placeId)).map { updRes =>
              logger.debug(s"Update partOf result $updRes")
              MusitSuccess(eventId)
            }
          }

        case err: MusitError => Future.successful(err)
      }
    }

    val eventuallyCurrent = unitDao.getAllById(mid, id)
    val eventuallyMaybeTo = unitDao.getAllById(mid, event.to.placeId)

    val eventuallyExistance = for {
      maybeCurrent <- eventuallyCurrent
      maybeTo <- eventuallyMaybeTo
    } yield (maybeCurrent, maybeTo)

    eventuallyExistance.flatMap {
      case (maybeCurrent: Option[StorageUnit], maybeTo: Option[StorageUnit]) =>
        maybeCurrent.flatMap { current =>
          maybeTo.map { to =>
            logger.debug(s"Going to move node $id from ${current.path} to ${to.path}")
            mv(current.path.get, to.path.get).map { res =>
              logger.debug(s"Updated $res entries")
              res
            }
          }
        }.getOrElse(
          Future.successful(MusitValidationError("Could not find to or from node."))
        )
    }
  }

  /**
   * TODO: Document me!
   */
  def moveObject(
    mid: MuseumId,
    objectId: Long,
    event: MoveObject
  )(implicit currUsr: String): Future[MusitResult[Long]] = {
    val dto = DtoConverters.MoveConverters.moveObjectToDto(event)
    move(mid, objectId, dto) { eventId =>
      Future.successful(MusitSuccess(eventId))
    }
  }
}
