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
import no.uio.musit.microservice.storageAdmin.domain.dto.{ StorageDtoConverter, StorageNodeDTO, StorageType }
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Organisation, Room, _ }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, Misc }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.Left

class StorageUnitService @Inject() (
    storageUnitDao: StorageUnitDao,
    roomService: RoomService,
    buildingService: BuildingService,
    organisationService: OrganisationService,
    storageDao: StorageDao

) extends Object with StorageDtoConverter {

  private def storageUnitTypeMismatch(id: Long, expected: StorageType, inDatabase: StorageType): MusitError =
    ErrorHelper.conflict(s"StorageUnit with id: $id was expected to have storage type: $expected, " +
      s"but had the type: $inDatabase in the database.")

  def createStorageUnit(storageUnit: StorageUnit): MusitFuture[StorageUnit] =
    storageUnitDao.insertStorageUnit(storageUnit).toMusitFuture // musitFutureMap(stNodeDto=> fromDto(stNodeDto))

  def createStorageTriple(storage: Storage): MusitFuture[Storage] = {
    storage match {
      case su: StorageUnit => createStorageUnit(su)
      case room: Room => roomService.create(room)
      case building: Building => buildingService.create(building)
      case organisation: Organisation => organisationService.create(organisation)
    }
  }

  def getChildren(id: Long): Future[Seq[Storage]] =
    storageUnitDao.getChildren(id).flatMap {
      list =>
        val res = list.map {
          storageNodeDto =>
            storageDao.getByNode(storageNodeDto)
        }
        Misc.filterSuccesses(res)
    }

  def getPath(id: Long): Future[Seq[StorageNodeCommonProperties]] = {
    storageUnitDao.getPath(id).map(_.map(__ => this.dtoToStorageNodeCommonProperties(__)))
  }

  def getById(id: Long): MusitFuture[Storage] = {
    storageDao.getById(id)
  }

  def getStorageType(id: Long): MusitFuture[StorageType] =
    storageUnitDao.getStorageType(id)

  def all: Future[Seq[StorageNodeDTO]] =
    storageUnitDao.all()

  def rootNodes(readGroup: String): Future[Seq[StorageNodeDTO]] =
    storageUnitDao.rootNodes(readGroup)

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) =
    storageUnitDao.updateStorageUnitAndMaybeEnvReq(id, storageUnit)

  def verifyStorageTypeMatchesDatabase(id: Long, expectedStorageUnitType: StorageType): MusitFuture[Boolean] =
    getStorageType(id).musitFutureFlatMapInnerEither {
      storageUnitTypeInDatabase =>
        boolToMusitBool(
          expectedStorageUnitType == storageUnitTypeInDatabase,
          storageUnitTypeMismatch(id, expectedStorageUnitType, storageUnitTypeInDatabase)
        )
    }

  def updateStorageTripleByID(id: Long, triple: Storage): Future[Either[MusitError, Int]] =
    verifyStorageTypeMatchesDatabase(id, StorageType.fromStorage(triple)).musitFutureFlatMap { _ =>
      triple match {
        case st: StorageUnit =>
          updateStorageUnitByID(id, st).toMusitFuture
        case building: Building =>
          buildingService.updateBuildingByID(id, building).toMusitFuture
        case room: Room =>
          roomService.updateRoomByID(id, room).toMusitFuture
        case organisation: Organisation =>
          organisationService.updateOrganisationByID(id, organisation).map(Right(_))
      }
    }

  def deleteStorageTriple(id: Long): MusitFuture[Int] =
    storageUnitDao.deleteStorageNode(id).toMusitFuture
  def setPartOf(id: Long, partOf: Long): Future[Either[MusitError, Boolean]] =
    storageUnitDao.setPartOf(id, partOf).map {
      case 1 => Right(true)
      case num => Left(MusitError(message = s"Failed while setting partOf=$partOf for id=$id. Got $num updated rows."))
    }
}
