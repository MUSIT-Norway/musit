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
package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao._
import no.uio.musit.microservice.storageAdmin.domain.dto.{ StorageType, StorageNodeDTO }
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Room, _ }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.Left

class StorageUnitService @Inject() (
    storageUnitDao: StorageUnitDao,
    roomService: RoomService,
    buildingService: BuildingService
) {

  private def storageUnitTypeMismatch(id: Long, expected: StorageType, inDatabase: StorageType): MusitError =
    ErrorHelper.conflict(s"StorageUnit with id: $id was expected to have storage type: $expected, " +
      s"but had the type: $inDatabase in the database.")

  def create(storageUnit: StorageNodeDTO): MusitFuture[Storage] =
    ServiceHelper.daoInsert(storageUnitDao.insert(storageUnit)).musitFutureMap(Storage.fromDTO)

  def createStorageTriple(storage: Storage): MusitFuture[Storage] = {
    val storageDTO = Storage.toDTO(storage)
    storage match {
      case su: StorageUnit => create(storageDTO)
      case room: Room => roomService.create(storageDTO, room)
      case building: Building => buildingService.create(storageDTO, building)
    }
  }

  def getChildren(id: Long): Future[Seq[Storage]] =
    storageUnitDao.getChildren(id).map(_.map(Storage.fromDTO))

  def getPath(id: Long): Future[Seq[Storage]] =
    storageUnitDao.getPath(id).map(_.map(Storage.fromDTO))

  private def getStorageUnitOnly(id: Long) =
    storageUnitDao.getStorageUnitOnlyById(id).toMusitFuture(storageUnitDao.storageUnitNotFoundError(id))

  private def getBuildingById(id: Long) =
    buildingService.getBuildingById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageBuilding with id: $id"))

  private def getRoomById(id: Long) =
    roomService.getRoomById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageRoom with id: $id"))

  def getById(id: Long): MusitFuture[Storage] = {
    val musitFutureStorageUnit = getStorageUnitOnly(id)
    musitFutureStorageUnit.musitFutureFlatMap { storageUnit =>
      storageUnit.storageType match {
        case StorageType.StorageUnit => MusitFuture.successful(Storage.fromDTO(storageUnit))
        case StorageType.Building => getBuildingById(id).musitFutureMap(storageBuilding => Storage.getBuilding(storageUnit, storageBuilding))
        case StorageType.Room => getRoomById(id).musitFutureMap(storageRoom => Storage.getRoom(storageUnit, storageRoom))
      }
    }
  }

  def getStorageType(id: Long): MusitFuture[StorageType] =
    storageUnitDao.getStorageType(id)

  def all: Future[Seq[StorageNodeDTO]] =
    storageUnitDao.all()

  def rootNodes(readGroup: String): Future[Seq[StorageNodeDTO]] =
    storageUnitDao.rootNodes(readGroup)

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) =
    storageUnitDao.updateStorageUnit(id, Storage.toDTO(storageUnit))

  def verifyStorageTypeMatchesDatabase(id: Long, expectedStorageUnitType: StorageType): MusitFuture[Boolean] =
    getStorageType(id).musitFutureFlatMapInnerEither {
      storageUnitTypeInDatabase =>
        boolToMusitBool(
          expectedStorageUnitType == storageUnitTypeInDatabase,
          storageUnitTypeMismatch(id, expectedStorageUnitType, storageUnitTypeInDatabase)
        )
    }

  def updateStorageTripleByID(id: Long, triple: Storage): Future[Either[MusitError, Int]] =
    verifyStorageTypeMatchesDatabase(id, StorageType.fromStorage(triple)).flatMap {
      case Right(true) =>
        triple match {
          case st: StorageUnit =>
            updateStorageUnitByID(id, st).map(Right(_))
          case building: Building =>
            buildingService.updateBuildingByID(id, building).map(Right(_))
          case room: Room =>
            roomService.updateRoomByID(id, room).map(Right(_))
        }
      case Left(error) =>
        Future.successful(Left(error))
    }

  def deleteStorageTriple(id: Long): MusitFuture[Int] =
    storageUnitDao.deleteStorageUnit(id).toMusitFuture

  def setPartOf(id: Long, partOf: Long): Future[Either[MusitError, Boolean]] =
    storageUnitDao.setPartOf(id, partOf).map {
      case 1 => Right(true)
      case num => Left(MusitError(message = s"Failed while setting partOf=$partOf for id=$id. Got $num updated rows."))
    }
}
