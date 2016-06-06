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

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Room, _ }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.ServiceHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import no.uio.musit.microservices.common.extensions.FutureExtensions._

trait StorageUnitService {

  def StorageUnitNotFoundError(id: Long): Either[MusitError, Nothing] = {
    ServiceHelper.badRequest(s"Unknown storageUnit with ID: $id")
  }

  def StorageRoomNotFoundError(id: Long): Either[MusitError, Nothing] = {
    ServiceHelper.badRequest(s"Unknown storageRoom with ID: $id")
  }

  def StorageBuildingNotFoundError(id: Long): Either[MusitError, Nothing] = {
    ServiceHelper.badRequest(s"Unknown storageBuilding with ID: $id")
  }

  def StorageUnitTypeMismatch(id: Long, expected: StorageUnitType, inDatabase: StorageUnitType): Either[MusitError, Nothing] = {
    ServiceHelper.badRequest(s"StorageUnit with ID: $id was expected to have storage type: ${expected.typename}, but had the type: ${inDatabase.typename} in the database.")
  }

  def create(storageUnit: StorageUnit): Future[Either[MusitError, StorageUnitTriple]] = {
    ServiceHelper.daoInsert(StorageUnitDao.insert(storageUnit)).map(_.right.map(StorageUnitTriple.createStorageUnit))
  }

  def createStorageTriple(storageTriple: StorageUnitTriple): Future[Either[MusitError, StorageUnitTriple]] = {
    val storageUnit = storageTriple.storageUnit
    storageTriple.storageKind match {
      case StUnit => create(storageUnit)
      case Room => RoomService.create(storageUnit, storageTriple.getRoom)
      case Building => BuildingService.create(storageUnit, storageTriple.getBuilding)
    }
  }

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    StorageUnitDao.getChildren(id)
  }

  private def getStorageUnitOnly(id: Long) = StorageUnitDao.getStorageUnitOnlyById(id).foldInnerOption(StorageUnitNotFoundError(id), Right(_))

  def getById(id: Long): Future[Either[MusitError, StorageUnitTriple]] = {
    val fEitherStorageUnit = getStorageUnitOnly(id)

    fEitherStorageUnit.futureEitherFlatMap { storageUnit =>
      storageUnit.storageKind match {
        case StUnit => Future.successful(Right(StorageUnitTriple.createStorageUnit(storageUnit)))
        case Building => StorageUnitDao.getBuildingById(id).foldInnerOption(StorageBuildingNotFoundError(id), storageBuilding => Right(StorageUnitTriple.createBuilding(storageUnit, storageBuilding)))
        case Room => StorageUnitDao.getRoomById(id).foldInnerOption(StorageRoomNotFoundError(id), storageRoom => Right(StorageUnitTriple.createRoom(storageUnit, storageRoom)))
      }
    }

  }

  def getStorageType(id: Long): Future[Option[StorageUnitType]] = StorageUnitDao.getStorageType(id)

  def all: Future[Seq[StorageUnit]] = {
    StorageUnitDao.all()
  }

  def find(id: Long) = {
    getById(id)
  }

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) = {
    ServiceHelper.daoUpdateById(StorageUnitDao.updateStorageUnit, id, storageUnit)
  }

  def verifyStorageTypeMatchesDatabase(id: Long, expectedStorageUnitType: StorageUnitType) = {
    def handleWithStorageType(storageUnitTypeInDatabase: StorageUnitType) = {
      if (expectedStorageUnitType == storageUnitTypeInDatabase) Right(())
      else StorageUnitTypeMismatch(id, expectedStorageUnitType, storageUnitTypeInDatabase)

    }
    getStorageType(id).foldInnerOption(StorageUnitNotFoundError(id), handleWithStorageType(_))
  }

  def updateStorageTripleByID(id: Long, triple: StorageUnitTriple) = {
    verifyStorageTypeMatchesDatabase(id, triple.storageKind).futureEitherFlatMap { _ =>

      val modifiedTriple = triple.copyWithId(id) //We want the id in the url to override potential mistake in the body (of the original http request).

      val storageUnit = modifiedTriple.storageUnit

      modifiedTriple.storageKind match {
        case StUnit => updateStorageUnitByID(id, storageUnit)
        case Building => BuildingService.updateBuildingByID(id, (storageUnit, modifiedTriple.getBuilding))
        case Room => RoomService.updateRoomByID(id, (storageUnit, modifiedTriple.getRoom))
      }

    }
  }
}

object StorageUnitService extends StorageUnitService {
}

trait RoomService {
  def create(storageUnit: StorageUnit, storageRoom: StorageRoom): Future[Either[MusitError, StorageUnitTriple]] = {
    ServiceHelper.daoInsert(StorageUnitDao.insertRoom(storageUnit, storageRoom))
  }

  def updateRoomByID(id: Long, storageUnitAndRoom: (StorageUnit, StorageRoom)) = {
    ServiceHelper.daoUpdateById(StorageUnitDao.updateRoom, id, storageUnitAndRoom)
  }
}

object RoomService extends RoomService

trait BuildingService {
  def create(storageUnit: StorageUnit, storageBuilding: StorageBuilding): Future[Either[MusitError, StorageUnitTriple]] = {
    ServiceHelper.daoInsert(StorageUnitDao.insertBuilding(storageUnit, storageBuilding))
  }

  def updateBuildingByID(id: Long, storageUnitAndBuilding: (StorageUnit, StorageBuilding)) = {
    ServiceHelper.daoUpdateById(StorageUnitDao.updateBuildingByID, id, storageUnitAndBuilding)
  }
}

object BuildingService extends BuildingService

