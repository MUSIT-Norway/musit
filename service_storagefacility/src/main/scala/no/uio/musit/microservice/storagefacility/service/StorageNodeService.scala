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
import no.uio.musit.microservice.storagefacility.dao.storage.{ BuildingDao, OrganisationDao, RoomDao, StorageUnitDao }
import no.uio.musit.microservice.storagefacility.domain.MusitResults._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageNodeDto
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject() (
    val storageUnitDao: StorageUnitDao,
    val roomDao: RoomDao,
    val buildingDao: BuildingDao,
    val organisationDao: OrganisationDao
) {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * TODO: Document me!
   */
  def addStorageUnit(storageUnit: StorageUnit): Future[StorageUnit] = {
    storageUnitDao.insert(storageUnit)
  }

  /**
   * TODO: Document me!!!
   */
  def addRoom(storageRoom: Room): Future[Room] = {
    roomDao.insert(storageRoom)
  }

  /**
   * TODO: Document me!!!
   */
  def addBuilding(building: Building): Future[Building] = {
    buildingDao.insert(building)
  }

  /**
   * TODO: Document me!!!
   */
  def addOrganisation(organisation: Organisation): Future[Organisation] = {
    organisationDao.insert(organisation)
  }

  /**
   * TODO: Document me!
   */
  def updateStorageUnit(
    id: StorageNodeId,
    storageUnit: StorageUnit
  ): Future[MusitResult[Option[StorageUnit]]] = {
    storageUnitDao.update(id, storageUnit).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def updateRoom(id: StorageNodeId, room: Room): Future[MusitResult[Option[Room]]] = {
    roomDao.update(id, room).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def updateBuilding(
    id: StorageNodeId,
    building: Building
  ): Future[MusitResult[Option[Building]]] = {
    buildingDao.update(id, building).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def updateOrganisation(
    id: StorageNodeId,
    organisation: Organisation
  ): Future[MusitResult[Option[Organisation]]] = {
    organisationDao.update(id, organisation).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!
   */
  def getStorageUnitById(
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageUnit]]] =
    storageUnitDao.getById(id).map(maybeRes => MusitSuccess(maybeRes))

  /**
   * TODO: Document me!!!
   */
  def getRoomById(id: StorageNodeId): Future[MusitResult[Option[Room]]] = {
    roomDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(id: StorageNodeId): Future[MusitResult[Option[Building]]] = {
    buildingDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!!!
   */
  def getOrganisationById(id: StorageNodeId): Future[MusitResult[Option[Organisation]]] = {
    organisationDao.getById(id).map(MusitSuccess.apply)
  }

  /**
   * TODO: Document me!
   */
  def getNodeById(
    id: StorageNodeId
  ): Future[MusitResult[Option[StorageNode]]] = {
    storageUnitDao.getNodeById(id).flatMap { maybeNode =>
      maybeNode.map { node =>
        node.storageType match {
          case StorageType.StorageUnitType =>
            Future.successful {
              MusitSuccess(Option(StorageNodeDto.toStorageUnit(node)))
            }

          case StorageType.BuildingType =>
            getBuildingById(id)

          case StorageType.RoomType =>
            getRoomById(id)

          case StorageType.OrganisationType =>
            getOrganisationById(id)

        }
      }.getOrElse(Future.successful(MusitSuccess[Option[StorageNode]](None)))
    }

  }

  /**
   * TODO: Document me!
   */
  def getChildren(id: StorageNodeId): Future[Seq[StorageNode]] = {
    storageUnitDao.getChildren(id).map(_.map(StorageNodeDto.toStorageNode))
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

      case err: MusitError[_] =>
        false
    }
  }

  //  /**
  //   * TODO: Document me! + id: Long should be id: StorageNodeId
  //   */
  //  def updateStorageTripleById(
  //    id: Long,
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
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def deleteNode(id: StorageNodeId): Future[MusitResult[Int]] = {
    storageUnitDao.nodeExists(id).flatMap {
      case MusitSuccess(exists) =>
        if (exists) storageUnitDao.markAsDeleted(id)
        else Future.successful(MusitSuccess(0))
    }
  }
}
