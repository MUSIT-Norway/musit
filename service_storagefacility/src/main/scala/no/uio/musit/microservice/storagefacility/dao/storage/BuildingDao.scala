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
import no.uio.musit.microservice.storagefacility.dao.ColumnTypeMappers
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import no.uio.musit.microservice.storagefacility.domain.storage.{ Building, Storage, StorageNodeId }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
@Singleton
class BuildingDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val BuildingTable = TableQuery[BuildingTable]

  /**
   * TODO: Document me!!!
   */
  def getBuildingById(id: StorageNodeId): Future[Option[Building]] = {
    val action = BuildingTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  /**
   * TODO: Document me!!!
   */
  def updateBuildingOnlyAction(id: StorageNodeId, storageBuilding: Building): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  /**
   * TODO: Document me!!!
   */
  def updateBuilding(id: StorageNodeId, building: Building) = {
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageUnitAction(id, Storage.toDTO(building))
    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ =>
      updateBuildingOnlyAction(id, building.copy(id = Some(id)))
    }
    db.run(combinedAction.transactionally)
  }

  private def insertBuildingOnlyAction(storageBuilding: Building): DBIO[Int] = {
    val stBuilding = storageBuilding.copy(links = Storage.linkText(storageBuilding.id))
    val insertQuery = BuildingTable
    val action = insertQuery += stBuilding
    action
  }

  /**
   * TODO: Document me!!!
   */
  def insertBuilding(storageUnit: StorageUnitDto, storageBuilding: Building): Future[Storage] = {
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(storageUnit)
      n <- insertBuildingOnlyAction(storageBuilding.copy(id = storageUnit.id))
    } yield Storage.getBuilding(storageUnit, storageBuilding)).transactionally

    db.run(action)
  }

  private class BuildingTable(tag: Tag) extends Table[Building](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[StorageNodeId]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[StorageNodeId], address: Option[String]) =>
      Building(id, null, None, None, None, None, None, None, None, None, address)

    def destroy(building: Building) = Some(building.id, building.address)
  }

}

