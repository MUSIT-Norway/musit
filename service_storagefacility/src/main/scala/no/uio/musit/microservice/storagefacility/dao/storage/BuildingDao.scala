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

package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.MusitResults.{ MusitResult, MusitSuccess }
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{ BuildingDto, ExtendedStorageNode, StorageNodeDto }
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
@Singleton
class BuildingDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[BuildingDao])

  private val buildingTable = TableQuery[BuildingTable]

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[Building]] = {
    val action = for {
      maybeUnitDto <- storageUnitDao.getByIdAction(id)
      maybeBuildingDto <- buildingTable.filter(_.id === id).result.headOption
    } yield {
      // Map the results into an ExtendedStorageNode type
      maybeUnitDto.flatMap(u =>
        maybeBuildingDto.map(b => ExtendedStorageNode(u, b)))
    }
    // Execute the query
    db.run(action).map(_.map { unitBuildingTuple =>
      StorageNodeDto.toBuilding(unitBuildingTuple)
    })
  }

  private[dao] def updateAction(id: StorageNodeId, building: BuildingDto): DBIO[Int] = {
    buildingTable.filter(_.id === id).update(building)
  }

  /**
   * TODO: Document me!!!
   */
  def update(id: StorageNodeId, building: Building): Future[Option[Building]] = {
    val extendedBuildingDto = StorageNodeDto.fromBuilding(building)
    val action = for {
      unitsUpdated <- storageUnitDao.updateAction(id, extendedBuildingDto.storageUnitDto)
      buildingsUpdated <- updateAction(id, extendedBuildingDto.extension.copy(id = Some(id)))
    } yield buildingsUpdated

    db.run(action.transactionally).flatMap {
      case res: Int if res > 1 || res < 0 =>
        logger.warn("Wrong amount of rows updated")
        Future.successful(None)

      case res: Int =>
        getById(id)
    }
  }

  private[dao] def insertAction(buildingDto: BuildingDto): DBIO[BuildingDto] = {
    buildingTable returning buildingTable
      .map(_.id) into ((building, id) => building.copy(id = id)) += buildingDto
  }

  /**
   * TODO: Document me!!!
   */
  def insert(building: Building): Future[Building] = {
    val extendedDto = StorageNodeDto.fromBuilding(building)
    val action = for {
      storageUnit <- storageUnitDao.insertAction(extendedDto.storageUnitDto)
      inserted <- insertAction(extendedDto.extension.copy(id = storageUnit.id))
    } yield {
      val extNode = ExtendedStorageNode(storageUnit, inserted)
      StorageNodeDto.toBuilding(extNode)
    }

    db.run(action.transactionally)
  }

  private class BuildingTable(
      val tag: Tag
  ) extends Table[BuildingDto](tag, SchemaName, "BUILDING") {

    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[StorageNodeId]]("STORAGE_UNIT_ID", O.PrimaryKey)
    val address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[StorageNodeId], address: Option[String]) =>
      BuildingDto(
        id = id,
        address = address
      )

    def destroy(building: BuildingDto) =
      Some((building.id, building.address))
  }

}

