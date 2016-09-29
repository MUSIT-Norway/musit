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
import no.uio.musit.microservice.storagefacility.domain.NodeStats
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.dto.{BaseEventDto, DtoConverters}
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveEvent, MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject() (
    val storageUnitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val organisationDao: OrganisationDao,
    val envReqService: EnvironmentRequirementService,
    val eventDao: EventDao,
    val localObjectDao: LocalObjectDao,
    val statsDao: StorageStatsDao
) {

  val logger = Logger(classOf[StorageNodeService])

  private[service] def saveEnvReq(
    nodeId: StorageNodeId,
    envReq: EnvironmentRequirement
  )(implicit currUsr: String): Future[Option[EnvironmentRequirement]] = {
    val now = dateTimeNow
    val er = EnvRequirement.toEnvRequirementEvent(nodeId, now, envReq)

    envReqService.add(er).map {
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

  def addRoot(root: Root): Future[Root] = storageUnitDao.insertRoot(root)

  /**
   * TODO: Document me!
   */
  def addStorageUnit(storageUnit: StorageUnit)(implicit currUsr: String): Future[StorageUnit] = {
    storageUnitDao.insert(storageUnit).flatMap { sn =>
      val maybeWithEnvReq =
        for {
          nodeId <- sn.id
          envReq <- storageUnit.environmentRequirement
        } yield {
          logger.debug(s"Saving new environment requirement data for unit $nodeId")
          saveEnvReq(nodeId, envReq).map { maybeEnvReq =>
            sn.copy(environmentRequirement = maybeEnvReq)
          }
        }
      maybeWithEnvReq.getOrElse(Future.successful(sn))
    }
  }

  /**
   * TODO: Document me!!!
   */
  def addRoom(storageRoom: Room)(implicit currUsr: String): Future[Room] = {
    roomDao.insert(storageRoom).flatMap { addedRoom =>
      logger.debug(s"Room was added with id ${addedRoom.id}")
      val maybeWithEnvReq =
        for {
          nodeId <- addedRoom.id
          envReq <- storageRoom.environmentRequirement
        } yield {
          logger.debug(s"Saving new environment requirement data for room $nodeId")
          saveEnvReq(nodeId, envReq).map { maybeEnvReq =>
            addedRoom.copy(environmentRequirement = maybeEnvReq)
          }
        }
      maybeWithEnvReq.getOrElse(Future.successful(addedRoom))
    }
  }

  /**
   * TODO: Document me!!!
   */
  def addBuilding(building: Building)(implicit currUsr: String): Future[Building] = {
    buildingDao.insert(building).flatMap { addedBuilding =>
      val maybeWithEnvReq =
        for {
          nodeId <- addedBuilding.id
          envReq <- building.environmentRequirement
        } yield {
          logger.debug(s"Saving new environment requirement data for building $nodeId")
          saveEnvReq(nodeId, envReq).map { maybeEnvReq =>
            addedBuilding.copy(environmentRequirement = maybeEnvReq)
          }
        }
      maybeWithEnvReq.getOrElse(Future.successful(addedBuilding))
    }
  }

  /**
   * TODO: Document me!!!
   */
  def addOrganisation(organisation: Organisation)(implicit currUsr: String): Future[Organisation] = {
    organisationDao.insert(organisation).flatMap { addedOrgNode =>
      val maybeWithEnvReq =
        for {
          nodeId <- addedOrgNode.id
          envReq <- organisation.environmentRequirement
        } yield {
          logger.debug(s"Saving new environment requirement data for organisation node $nodeId")
          saveEnvReq(nodeId, envReq).map { maybeEnvReq =>
            addedOrgNode.copy(environmentRequirement = maybeEnvReq)
          }
        }
      maybeWithEnvReq.getOrElse(Future.successful(addedOrgNode))
    }
  }

  /**
   * TODO: Document me!
   */
  def updateStorageUnit(
    id: StorageNodeId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[MusitResult[Option[StorageUnit]]] = {
    storageUnitDao.update(id, storageUnit).flatMap { maybeUnit =>
      logger.debug(s"Successfully updated storage unit $id")
      val maybeWithEnvReq = for {
        su <- maybeUnit
        envReq <- storageUnit.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for unit node $id")
        saveEnvReq(id, envReq).map { er =>
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
    id: StorageNodeId,
    room: Room
  )(implicit currUsr: String): Future[MusitResult[Option[Room]]] = {
    roomDao.update(id, room).flatMap { maybeRoom =>
      logger.debug(s"Successfully updated storage room $id")
      val maybeWithEnvReq = for {
        r <- maybeRoom
        envReq <- room.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for room node $id")
        saveEnvReq(id, envReq).map { er =>
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
    id: StorageNodeId,
    building: Building
  )(implicit currUsr: String): Future[MusitResult[Option[Building]]] = {
    buildingDao.update(id, building).flatMap { maybeBuilding =>
      logger.debug(s"Successfully updated storage building $id")
      val maybeWithEnvReq = for {
        b <- maybeBuilding
        envReq <- building.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for building node $id")
        saveEnvReq(id, envReq).map { er =>
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
    id: StorageNodeId,
    organisation: Organisation
  )(implicit currUsr: String): Future[MusitResult[Option[Organisation]]] = {
    organisationDao.update(id, organisation).flatMap { maybeOrg =>
      logger.debug(s"Successfully updated storage building $id")
      val maybeWithEnvReq = for {
        org <- maybeOrg
        envReq <- organisation.environmentRequirement
      } yield {
        logger.debug(s"Saving new environment requirement data for organisation node $id")
        saveEnvReq(id, envReq).map { er =>
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
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] = {
    for {
      unitRes <- storageUnitDao.getById(id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(id)
    } yield {
      unitRes.map { maybeUnit =>
        maybeUnit.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getRoomById(id: StorageNodeId): Future[MusitResult[Option[Room]]] = {
    for {
      roomRes <- roomDao.getById(id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(id)
    } yield {
      roomRes.map { maybeRoom =>
        maybeRoom.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(id: StorageNodeId): Future[MusitResult[Option[Building]]] = {
    for {
      buildingRes <- buildingDao.getById(id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(id)
    } yield {
      buildingRes.map { maybeBuilding =>
        maybeBuilding.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }

  }

  /**
   * TODO: Document me!!!
   */
  def getOrganisationById(id: StorageNodeId): Future[MusitResult[Option[Organisation]]] = {
    for {
      orgRes <- organisationDao.getById(id).map(MusitSuccess.apply)
      maybeEnvReq <- getEnvReq(id)
    } yield {
      orgRes.map { maybeOrg =>
        maybeOrg.map(_.copy(environmentRequirement = maybeEnvReq))
      }
    }
  }

  private def getEnvReq(id: StorageNodeId): Future[Option[EnvironmentRequirement]] = {
    envReqService.findLatestForNodeId(id).map {
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
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageNode]]] = {
    storageUnitDao.getStorageType(id).flatMap { res =>
      res.map { maybeType =>
        maybeType.map {
          case StorageType.RootType => Future.successful(MusitSuccess(None))
          case StorageType.OrganisationType => getOrganisationById(id)
          case StorageType.BuildingType => getBuildingById(id)
          case StorageType.RoomType => getRoomById(id)
          case StorageType.StorageUnitType => getStorageUnitById(id)
        }.getOrElse {
          Future.successful(MusitSuccess(None))
        }
      }.getOrElse(Future.successful(MusitSuccess[Option[StorageNode]](None)))
    }
  }

  /**
   * TODO: Document me!
   */
  def rootNodes: Future[Seq[StorageNode]] = storageUnitDao.findRootNodes

  /**
   * TODO: Document me!
   */
  def getChildren(id: StorageNodeId): Future[Seq[GenericStorageNode]] = {
    storageUnitDao.getChildren(id)
  }

  /**
   * TODO: Document me!
   */
  def getStorageType(id: StorageNodeId): Future[MusitResult[Option[StorageType]]] = {
    storageUnitDao.getStorageType(id)
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

  def nodeStats(nodeId: StorageNodeId): Future[MusitResult[Option[NodeStats]]] = {
    getNodeById(nodeId).flatMap {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { node =>
          val eventuallyTotal = statsDao.totalObjectCount(node)
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
  def deleteNode(id: StorageNodeId)(implicit currUsr: String): Future[MusitResult[Option[Int]]] = {
    getNodeById(id).flatMap {
      case MusitSuccess(maybeNode) =>
        maybeNode.map { node =>
          isEmpty(node).flatMap { empty =>
            if (empty) storageUnitDao.markAsDeleted(id).map(_.map(Some.apply))
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
  private def move(id: Long, dto: BaseEventDto)(f: Long => Future[MusitResult[Long]]): Future[MusitResult[Long]] = {
    eventDao.insertEvent(dto).flatMap(eventId => f(eventId)).recover {
      case NonFatal(ex) =>
        val msg = s"An exception occured trying to move $id"
        logger.error(msg, ex)
        MusitInternalError(msg)
    }
  }

  /**
   * TODO: Document me!
   */
  def moveNode(
    id: StorageNodeId,
    event: MoveNode
  )(implicit currUsr: String): Future[MusitResult[Long]] = {
    val dto = DtoConverters.MoveConverters.moveNodeToDto(event)
    move(id, dto) { eventId =>
      storageUnitDao.updatePartOf(id, Some(event.to.placeId)).map { updRes =>
        logger.debug(s"Update partOf result $updRes")
        MusitSuccess(eventId)
      }
    }
  }

  /**
   * TODO: Document me!
   */
  def moveObject(
    objectId: Long,
    event: MoveObject
  )(implicit currUsr: String): Future[MusitResult[Long]] = {
    val dto = DtoConverters.MoveConverters.moveObjectToDto(event)
    move(objectId, dto) { eventId =>
      Future.successful(MusitSuccess(eventId))
    }
  }
}
