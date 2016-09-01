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
import no.uio.musit.microservice.storagefacility.dao.storage.StorageUnitDao
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.Left

/**
 * TODO: Document me!!!
 */
class StorageUnitService @Inject() (
    storageUnitDao: StorageUnitDao,
    roomService: RoomService,
    buildingService: BuildingService
) {

  private def storageUnitTypeMismatch(
    id: StorageNodeId,
    expected: StorageType,
    inDatabase: StorageType
  ): MusitError =
    ErrorHelper.conflict(
      s"StorageUnit with id: $id was expected to have storage " +
        s"type: ${expected.entryName}, but had the " +
        s"type: ${inDatabase.entryName} in the database."
    )

  private def getStorageUnitOnly(id: StorageNodeId) = {
    storageUnitDao.getStorageUnitOnlyById(id)
      .toMusitFuture(
        // FIXME: This should be part of the getStorageUnitXXX dao method signature.
        storageUnitDao.storageUnitNotFoundError(id)
      )
  }

  private def getBuildingById(id: StorageNodeId) =
    buildingService.getBuildingById(id)
      .toMusitFuture(
        ErrorHelper.notFound(s"Unknown storageBuilding with id: $id")
      )

  private def getRoomById(id: StorageNodeId) =
    roomService.getRoomById(id)
      .toMusitFuture(ErrorHelper.notFound(s"Unknown storageRoom with id: $id"))

  /**
   * TODO: Document me!
   */
  def create(storageUnit: StorageUnitDto): MusitFuture[Storage] =
    ServiceHelper.daoInsert(storageUnitDao.insert(storageUnit)).musitFutureMap(Storage.fromDTO)

  /**
   * TODO: Document me!
   */
  def createStorageTriple(storage: Storage): MusitFuture[Storage] = {
    val storageDTO = Storage.toDTO(storage)
    storage match {
      case su: StorageUnit => create(storageDTO)
      case room: Room => roomService.create(storageDTO, room)
      case building: Building => buildingService.create(storageDTO, building)
    }
  }

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def getChildren(id: Long): Future[Seq[Storage]] =
    storageUnitDao.getChildren(StorageNodeId(id)).map(_.map(Storage.fromDTO))

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def getById(id: Long): MusitFuture[Storage] = {
    val sid = StorageNodeId(id)
    val musitFutureStorageUnit = getStorageUnitOnly(sid)

    musitFutureStorageUnit.musitFutureFlatMap { storageUnit =>
      storageUnit.`type` match {
        case StorageType.StorageUnit =>
          MusitFuture.successful(Storage.fromDTO(storageUnit))

        case StorageType.Building =>
          getBuildingById(sid).musitFutureMap { storageBuilding =>
            Storage.getBuilding(storageUnit, storageBuilding)
          }

        case StorageType.Room =>
          getRoomById(sid).musitFutureMap { storageRoom =>
            Storage.getRoom(storageUnit, storageRoom)
          }
      }
    }
  }

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def getStorageType(id: Long): MusitFuture[StorageType] =
    storageUnitDao.getStorageType(StorageNodeId(id))

  /**
   * TODO: Document me!
   */
  def all: Future[Seq[StorageUnitDto]] = storageUnitDao.all()

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def updateStorageUnitById(id: Long, storageUnit: StorageUnit) =
    storageUnitDao.updateStorageUnit(StorageNodeId(id), Storage.toDTO(storageUnit))

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def verifyStorageTypeMatchesDatabase(
    id: Long,
    expectedStorageUnitType: StorageType
  ): MusitFuture[Boolean] = {
    val sid = StorageNodeId(id)
    getStorageType(sid).musitFutureFlatMapInnerEither {
      storageUnitTypeInDatabase =>
        boolToMusitBool(
          expectedStorageUnitType == storageUnitTypeInDatabase,
          storageUnitTypeMismatch(sid, expectedStorageUnitType, storageUnitTypeInDatabase)
        )
    }
  }

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def updateStorageTripleById(
    id: Long,
    triple: Storage
  ): Future[Either[MusitError, Int]] = {
    val sid = StorageNodeId(id)
    verifyStorageTypeMatchesDatabase(sid, triple.storageType).flatMap {
      case Right(true) =>
        triple match {
          case st: StorageUnit =>
            updateStorageUnitById(sid, st).map(Right(_))
          case building: Building =>
            buildingService.updateBuildingByID(sid, building).map(Right(_))
          case room: Room =>
            roomService.updateRoomByID(sid, room).map(Right(_))
        }
      case Left(error) =>
        Future.successful(Left(error))
    }
  }

  /**
   * TODO: Document me! + id: Long should be id: StorageNodeId
   */
  def deleteStorageTriple(id: Long): MusitFuture[Int] =
    storageUnitDao.deleteStorageUnit(StorageNodeId(id)).toMusitFuture
}
