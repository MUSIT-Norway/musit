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
import no.uio.musit.microservice.storagefacility.dao.event.{ EventDao, LocalObjectDao }
import no.uio.musit.microservice.storagefacility.dao.storage.{ BuildingDao, OrganisationDao, RoomDao, StorageUnitDao }
import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.datetime._
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ BaseEventDto, DtoConverters }
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{ MoveEvent, MoveNode, MoveObject }
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
    val localObjectDao: LocalObjectDao
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

  def addRoot(mid: MuseumId, root: Root): Future[Root] = storageUnitDao.insertRoot(mid, root)

  /**
   * TODO: Document me!
   */
  def addStorageUnit(mid: MuseumId, storageUnit: StorageUnit)(implicit currUsr: String): Future[StorageUnit] = {
    storageUnitDao.insert(mid, storageUnit).flatMap { sn =>
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
  def addRoom(mid: MuseumId, storageRoom: Room)(implicit currUsr: String): Future[Room] = {
    roomDao.insert(mid, storageRoom).flatMap { addedRoom =>
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
  def addBuilding(mid: MuseumId, building: Building)(implicit currUsr: String): Future[Building] = {
    buildingDao.insert(mid, building).flatMap { addedBuilding =>
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
  def addOrganisation(mid: MuseumId, organisation: Organisation)(implicit currUsr: String): Future[Organisation] = {
    organisationDao.insert(mid, organisation).flatMap { addedOrgNode =>
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
    mid: MuseumId,
    id: StorageNodeId,
    storageUnit: StorageUnit
  )(implicit currUsr: String): Future[MusitResult[Option[StorageUnit]]] = {
    storageUnitDao.update(mid, id, storageUnit).flatMap { maybeUnit =>
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
  def rootNodes(mid: Int): Future[Seq[StorageNode]] = storageUnitDao.findRootNodes(mid)

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

  //  def updateStorageTripleById(
  //    id: StorageNodeId,
  //    triple: StorageNode
  //  ): Future[MusitResult[Int]] = {
  //    val sid = StorageNodeId(id)
  //    verifyStorageTypeMatchesDatabase(sid, triple.storageType).flatMap {
  //      case Right(true) =>
  //        triple match {
  //          case st: StorageUnit =>
  //            updateStorageUnitById(sid, st).map(Right(_))
  //          case building: Building =>
  //            buildingService.updateBuilding(sid, building).map(Right(_))
  //          case room: Room =>
  //            roomService.update(sid, room).map(Right(_))
  //        }
  //      case Left(error) =>
  //        Future.successful(Left(error))
  //    }
  //  }

  /**
   * TODO: Document me!
   */
  def deleteNode(id: StorageNodeId)(implicit currUsr: String): Future[MusitResult[Int]] = {
    storageUnitDao.nodeExists(id).flatMap {
      case MusitSuccess(exists) =>
        if (exists) storageUnitDao.markAsDeleted(id)
        else Future.successful(MusitSuccess(0))

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
