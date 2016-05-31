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
import no.uio.musit.microservice.storageAdmin.domain.{ StorageBuilding, StorageRoom, StorageUnit, StorageUnitType }
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch }
import no.uio.musit.microservices.common.utils.ServiceHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import no.uio.musit.microservices.common.utils.ServiceHelper._

trait StorageUnitService {

  def create(storageUnit: StorageUnit): Future[Either[MusitError, StorageUnit]] = {
    val newStorageUnitF = StorageUnitDao.insert(storageUnit)
    val value: Future[Either[MusitError, StorageUnit]] = newStorageUnitF.map { newStorageUnit =>
      Right(newStorageUnit)
    }
    value.recover {
      case _ => Left(MusitError(400, "va da feil??"))
    }
  }

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    StorageUnitDao.getChildren(id)
  }

  def getById(id: Long): Future[Option[StorageUnit]] = {
    StorageUnitDao.getById(id)
  }

  def getStorageType(id: Long): Future[Option[StorageUnitType]] = StorageUnitDao.getStorageType(id)

  def all: Future[Seq[StorageUnit]] = {
    StorageUnitDao.all()
  }

  def find(id: Long): Future[Option[StorageUnit]] = {
    getById(id)
  }

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) = {
    println(s"storageUnitSservice.updateStorageUnitByID $id")
    ServiceHelper.daoUpdateById(StorageUnitDao.updateStorageUnitByID, id, storageUnit)
    //#OLD: StorageUnitDao.updateStorageUnitByID(id, storageUnit)
  }

  /*def updateStorageUnitName(id: Long, storageName: String): Future[StorageUnit] = {
    StorageUnitDao.updateStorageNameByID(id, storageName)
  }*/

}

object StorageUnitService extends StorageUnitService

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
}

object BuildingService extends BuildingService

