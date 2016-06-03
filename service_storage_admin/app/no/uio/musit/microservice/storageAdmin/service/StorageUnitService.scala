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
import no.uio.musit.microservice.storageAdmin.domain.{Building, Room, _}
import no.uio.musit.microservices.common.domain.{MusitError, MusitSearch}
import no.uio.musit.microservices.common.utils.{ResourceHelper, ServiceHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import no.uio.musit.microservices.common.utils.ServiceHelper._
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import play.api.libs.json.Json

trait StorageUnitService {

  def StorageUnitNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageUnit with ID: $id")
  }

  def StorageRoomNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageRoom with ID: $id")
  }

  def StorageBuildingNotFoundError(id: Long): MusitError = {
    ServiceHelper.badRequest(s"Unknown storageRoom with ID: $id")
  }

  def StorageUnitTypeMismatch(id: Long, expected: StorageUnitType, inDatabase: StorageUnitType): MusitError = {
    ServiceHelper.badRequest(s"StorageUnit with ID: $id was expected to have storage type: ${expected.typename}, but had the type: ${inDatabase.typename} in the database.")
  }

  def create(storageUnit: StorageUnit): Future[Either[MusitError, StorageUnit]] = {
    val newStorageUnitF = StorageUnitDao.insertAndRun(storageUnit)
    val value: Future[Either[MusitError, StorageUnit]] = newStorageUnitF.map { newStorageUnit =>
      Right(newStorageUnit)
    }
    value.recover {
      case _ => Left(MusitError(400, "va da feil??"))
    }
  }

  def createStorageTriple(storageTriple: StorageUnitTriple): Future[Either[MusitError, StorageUnitTriple]] = {
    val storageUnit = storageTriple.storageUnit
    storageTriple.storageKind match {
      case StUnit => create(storageUnit).map(_.right.map(StorageUnitTriple.createStorageUnit))
      case Room => RoomService.create(storageUnit, storageTriple.getRoom).map(_.right.map(pair => StorageUnitTriple.createRoom(pair._1, pair._2)))
      case Building => BuildingService.create(storageUnit, storageTriple.getBuilding).map(_.right.map(pair => StorageUnitTriple.createBuilding(pair._1, pair._2)))
    }
  }

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    StorageUnitDao.getChildren(id)
  }

  def getStorageUnitTriple(id: Long, storageUnit: StorageUnit): Future[Either[MusitError, StorageUnitTriple]] =
    storageUnit.storageKind match {
      case StUnit =>
        Future.successful(Right(StorageUnitTriple.createStorageUnit(storageUnit)))

      case Building =>
        StorageUnitDao.getBuildingById(id).foldInnerOption(
          Left(StorageBuildingNotFoundError(id)),
          storageBuilding => Right(StorageUnitTriple.createBuilding(
            storageUnit,
            storageBuilding
          ))
        )

      case Room =>
        StorageUnitDao.getRoomById(id).foldInnerOption(
          Left(StorageRoomNotFoundError(id)),
          storageRoom => Right(StorageUnitTriple.createRoom(
            storageUnit,
            storageRoom
          ))
        )
    }

  def getById(id: Long): Future[Either[MusitError, StorageUnitTriple]] = {
    StorageUnitDao.getStorageUnitOnlyById(id).flatMap {
      case Some(storageUnit) => getStorageUnitTriple(id, storageUnit)
      case None => Future.successful(Left(StorageBuildingNotFoundError(id)))
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
    ServiceHelper.daoUpdateById(StorageUnitDao.updateStorageUnitByID, id, storageUnit)
  }

  def verifyStorageTypeEquality(id: Long, expectedStorageUnitType: StorageUnitType): Future[Boolean] = {
    def handleWithStorageType(storageUnitTypeInDatabase: StorageUnitType): Boolean = {

      else
    }
    getStorageType(id).foldInnerOption[](Left(StorageUnitNotFoundError(id)), (storageUnitTypeInDatabase) =>
      if (expectedStorageUnitType == storageUnitTypeInDatabase) Right(true)
      else Left(StorageUnitTypeMismatch(id, expectedStorageUnitType, storageUnitTypeInDatabase))
    )
  }

  def updateStorageTripleByID(id: Long, triple: StorageUnitTriple) = {
    val res = verifyStorageTypeEquality(id, triple.storageKind).mapOnInnerRight { _ =>

      val modifiedTriple = triple.copyWithId(id) //We want the id in the url to override potential mistake in the body (of the original http request).

      val storageUnit = modifiedTriple.storageUnit
      //We may also want to check that we're not modifying the storageType (in comparison to what is already in the database), but this is left as an exercise for the reader... ;)
      modifiedTriple.storageKind match {
        case StUnit => updateStorageUnitByID(id, storageUnit)
        case Building => BuildingService.updateBuildingByID(id, (storageUnit, modifiedTriple.getBuilding))
        case Room => RoomService.updateRoomByID(id, (storageUnit, modifiedTriple.getRoom))
      }

    }
    res |> flattenFutureEitherFutureEither
  }
}

object StorageUnitService extends StorageUnitService {
}

trait RoomService {
  def create(storageUnit: StorageUnit, storageRoom: StorageRoom): Future[Either[MusitError, (StorageUnit, StorageRoom)]] = {
    val newStorageRoomF = StorageUnitDao.insertRoom(storageUnit, storageRoom)
    val value: Future[Either[MusitError, (StorageUnit, StorageRoom)]] = newStorageRoomF.map { newStorageAndRoom =>
      Right(newStorageAndRoom)
    }
    value.recover {
      case _ => Left(MusitError(400, "va da feil??"))
    }
  }

  def updateRoomByID(id: Long, storageUnitAndRoom: (StorageUnit, StorageRoom)) = {
    ServiceHelper.daoUpdateById(StorageUnitDao.updateRoomByID, id, storageUnitAndRoom)
  }
}

object RoomService extends RoomService

trait BuildingService {
  def create(storageUnit: StorageUnit, storageBuilding: StorageBuilding): Future[Either[MusitError, (StorageUnit, StorageBuilding)]] = {
    val newStorageBuildingF = StorageUnitDao.insertBuilding(storageUnit, storageBuilding)
    val value: Future[Either[MusitError, (StorageUnit, StorageBuilding)]] = newStorageBuildingF.map { newStorageAndBuilding =>
      Right(newStorageAndBuilding)
    }
    value.recover {
      case _ => Left(MusitError(400, "va da feil??"))
    }
  }

  def updateBuildingByID(id: Long, storageUnitAndBuilding: (StorageUnit, StorageBuilding)) = {
    ServiceHelper.daoUpdateById(StorageUnitDao.updateBuildingByID, id, storageUnitAndBuilding)
  }
}

object BuildingService extends BuildingService

