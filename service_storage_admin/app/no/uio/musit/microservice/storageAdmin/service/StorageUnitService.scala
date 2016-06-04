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
import no.uio.musit.microservices.common.domain.{ MusitError, MusitStatusMessage }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper
import org.apache.http.HttpStatus
import play.api.http.Status
import play.mvc.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait StorageUnitService {

  def storageUnitNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageUnit with ID: $id")
  }

  def storageRoomNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageRoom with ID: $id")
  }

  def storageBuildingNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageRoom with ID: $id")
  }

  def storageUnitTypeMismatch(id: Long, expected: StorageUnitType, inDatabase: StorageUnitType): MusitError = {
    ServiceHelper.badRequest(s"StorageUnit with ID: $id was expected to have storage type: ${expected.typename}, " +
      s"but had the type: ${inDatabase.typename} in the database.")
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

  def getSideTable(id: Long, storageUnit: StorageUnit) =
    storageUnit.storageKind match {
      case StUnit =>
        Future.successful(Right(StorageUnitTriple.createStorageUnit(storageUnit)))

      case Building =>
        StorageUnitDao.getBuildingById(id).foldInnerOption(
          Left(storageBuildingNotFoundError(id)),
          storageBuilding => Right(StorageUnitTriple.createBuilding(
            storageUnit,
            storageBuilding
          ))
        )

      case Room =>
        StorageUnitDao.getRoomById(id).foldInnerOption(
          Left(storageRoomNotFoundError(id)),
          storageRoom => Right(StorageUnitTriple.createRoom(
            storageUnit,
            storageRoom
          ))
        )
    }

  def getByIdOnly(id: Long): Future[Either[MusitError, StorageUnitTriple]] = {
    StorageUnitDao.getStorageUnitOnlyById(id).flatMap {
      case Some(storageUnit) => getSideTable(id, storageUnit)
      case None => Future.successful(Left(storageUnitNotFoundError(id)))
    }
  }

  def updateStorageTripleByID(id: Long, triple: StorageUnitTriple): Future[Either[MusitError, MusitStatusMessage]] = {
    StorageUnitDao.getStorageType(id).flatMap {
      case Some(storageUnitTypeInDatabase) =>
        if (triple.storageKind == storageUnitTypeInDatabase) {
          updateTables(id, triple.copyWithId(id))
        } else {
          Future.successful(Left(storageUnitTypeMismatch(id, triple.storageKind, storageUnitTypeInDatabase)))
        }
      case None =>
        Future.successful(Left(storageUnitNotFoundError(id)))
    }
  }

  private def updateTables(id: Long, triple: StorageUnitTriple) = {
    triple.storageKind match {
      case StUnit =>
        StorageUnitDao.updateStorageUnit(id, triple.storageUnit).map {
          case 1 => Right(MusitStatusMessage("Record was updated!"))
          case other => Left(MusitError(
            status = Status.INTERNAL_SERVER_ERROR,
            message = "Could not update storage unit",
            developerMessage = "Updated rows: " + other
          ))
        }
      case Building =>
        BuildingService.updateBuildingByID(id, triple.storageUnit, triple.getBuilding)
      case Room =>
        RoomService.updateRoomByID(id, triple.storageUnit, triple.getRoom)
    }
  }
}

object StorageUnitService extends StorageUnitService

trait RoomService {
  def create(storageUnit: StorageUnit, storageRoom: StorageRoom) =
    ServiceHelper.daoInsert(StorageUnitDao.insertRoom(storageUnit, storageRoom))

  def updateRoomByID(id: Long, storageUnit: StorageUnit, storageRoom: StorageRoom) =
    StorageUnitDao.updateRoom(id, storageUnit, storageRoom).map(_ => Right(MusitStatusMessage("Record was updated!")))
}

object RoomService extends RoomService

trait BuildingService {
  def create(storageUnit: StorageUnit, storageBuilding: StorageBuilding) =
    ServiceHelper.daoInsert(StorageUnitDao.insertBuilding(storageUnit, storageBuilding))

  def badRequest(text: String, devMessage: String = "") =
    MusitError(Status.BAD_REQUEST, text, devMessage)

  def updateBuildingByID(id: Long, storageUnit: StorageUnit, storageBuilding: StorageBuilding) =
    StorageUnitDao.updateBuildingByID(id, storageUnit, storageBuilding).map(_ => Right(MusitStatusMessage("Record was updated!")))
}

object BuildingService extends BuildingService

