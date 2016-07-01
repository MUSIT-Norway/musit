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

import no.uio.musit.microservice.storageAdmin.dao._
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Room, _ }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }

import scala.concurrent.Future

trait StorageUnitService {
  //A separate function for this message because we want to verify we get this error message in some of the integration tests

  private def storageRoomNotFoundError(id: Long): MusitError = {
    ErrorHelper.notFound(s"Unknown storageRoom with id: $id")
  }

  private def storageBuildingNotFoundError(id: Long): MusitError = {
    ErrorHelper.notFound(s"Unknown storageBuilding with id: $id")
  }

  private def storageUnitTypeMismatch(id: Long, expected: StorageType, inDatabase: StorageType): MusitError = {
    ErrorHelper.conflict(s"StorageUnit with id: $id was expected to have storage type: ${expected.entryName}, " +
      s"but had the type: ${inDatabase.entryName} in the database.")
  }

  def create(storageUnit: StorageUnit): MusitFuture[StorageUnit] = {
    ServiceHelper.daoInsert(StorageUnitDao.insert(storageUnit)).musitFutureMap(st => st)
  }

  def createStorageTriple(storageTriple: Storage): MusitFuture[Storage] = {
    storageTriple match {
      case st: StorageUnit => create(st)
      case r: Room => RoomService.create(r.toStorageUnit.setType(StorageType.Room), r)
      case b: Building => BuildingService.create(b.toStorageUnit.setType(StorageType.Building), b)
    }
  }

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    StorageUnitDao.getChildren(id)
  }

  private def getStorageUnitOnly(id: Long) = StorageUnitDao.getStorageUnitOnlyById(id).toMusitFuture(StorageUnitDao.storageUnitNotFoundError(id))

  private def getBuildingById(id: Long) = BuildingDao.getBuildingById(id).toMusitFuture(storageBuildingNotFoundError(id))

  private def getRoomById(id: Long) = RoomDao.getRoomById(id).toMusitFuture(storageRoomNotFoundError(id))

  def getById(id: Long): MusitFuture[Storage] = {
    val musitFutureStorageUnit = getStorageUnitOnly(id)
    musitFutureStorageUnit.musitFutureFlatMap { storageUnit =>
      println("Getting " + storageUnit.storageType)
      storageUnit.storageType match {
        case StorageType.StorageUnit => MusitFuture.successful(storageUnit)
        case StorageType.Building => getBuildingById(id).musitFutureMap(storageBuilding => Storage.getBuilding(storageUnit.setType(StorageType.Building), storageBuilding))
        case StorageType.Room => getRoomById(id).musitFutureMap(storageRoom => Storage.getRoom(storageUnit.setType(StorageType.Room), storageRoom))
      }
    }
  }

  def getStorageType(id: Long): MusitFuture[StorageType] = StorageUnitDao.getStorageType(id)

  def all: Future[Seq[StorageUnit]] = {
    StorageUnitDao.all()
  }

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) = {
    ServiceHelper.daoUpdate(StorageUnitDao.updateStorageUnit, id, storageUnit)
  }

  def verifyStorageTypeMatchesDatabase(id: Long, expectedStorageUnitType: StorageType): MusitFuture[Boolean] = {
    getStorageType(id).musitFutureFlatMapInnerEither {
      storageUnitTypeInDatabase =>
        boolToMusitBool(
          expectedStorageUnitType == storageUnitTypeInDatabase,
          storageUnitTypeMismatch(id, expectedStorageUnitType, storageUnitTypeInDatabase)
        )
    }
  }

  def updateStorageTripleByID(id: Long, triple: Storage) = {
    verifyStorageTypeMatchesDatabase(id, triple.storageType).musitFutureFlatMap { _ =>
      triple match {
        case st: StorageUnit => updateStorageUnitByID(id, st)
        case b: Building => BuildingService.updateBuildingByID(id, (b.toStorageUnit.setType(StorageType.Building), b))
        case r: Room => RoomService.updateRoomByID(id, (r.toStorageUnit.setType(StorageType.Room), r))
      }
    }
  }

  def deleteStorageTriple(id: Long): MusitFuture[Int] =
    StorageUnitDao.deleteStorageUnit(id).toMusitFuture
}

object StorageUnitService extends StorageUnitService {
}

trait RoomService {
  def create(storageUnit: StorageUnit, storageRoom: Room): MusitFuture[Storage] = {
    ServiceHelper.daoInsert(RoomDao.insertRoom(storageUnit, storageRoom))
  }

  def updateRoomByID(id: Long, storageUnitAndRoom: (StorageUnit, Room)) = {
    ServiceHelper.daoUpdate(RoomDao.updateRoom, id, storageUnitAndRoom)
  }
}

object RoomService extends RoomService

trait BuildingService {
  def create(storageUnit: StorageUnit, storageBuilding: Building): MusitFuture[Storage] = {
    ServiceHelper.daoInsert(BuildingDao.insertBuilding(storageUnit, storageBuilding))
  }

  def updateBuildingByID(id: Long, storageUnitAndBuilding: (StorageUnit, Building)) = {
    ServiceHelper.daoUpdate(BuildingDao.updateBuilding, id, storageUnitAndBuilding)
  }
}

object BuildingService extends BuildingService

