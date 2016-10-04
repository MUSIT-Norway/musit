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

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{BuildingDto, ExtendedStorageNode, StorageNodeDto}
import no.uio.musit.service.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * TODO: Document me!!!
 */
@Singleton
class BuildingDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[BuildingDao])

  private val buildingTable = TableQuery[BuildingTable]

  private def updateAction(id: StorageNodeId, building: BuildingDto): DBIO[Int] = {
    buildingTable.filter(_.id === id).update(building)
  }

  private def insertAction(buildingDto: BuildingDto): DBIO[Int] = {
    buildingTable += buildingDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(mid: MuseumId, id: StorageNodeId): Future[Option[Building]] = {
    val action = for {
      maybeUnitDto <- getUnitByIdAction(mid, id)
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

  /**
   * TODO: Document me!!!
   */
  def update(mid: MuseumId, id: StorageNodeId, building: Building): Future[Option[Building]] = {
    val extendedBuildingDto = StorageNodeDto.fromBuilding(mid, building, Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(mid, id, extendedBuildingDto.storageUnitDto)
      buildingsUpdated <- if (unitsUpdated > 0) updateAction(id, extendedBuildingDto.extension) else DBIO.successful[Int](0)
    } yield buildingsUpdated

    db.run(action.transactionally).flatMap {
      case res: Int if res == 1 =>
        getById(mid, id)

      case res: Int =>
        logger.warn("Wrong amount of rows updated")
        Future.successful(None)
    }.recover {
      case NonFatal(ex) =>
        logger.debug(s"Using $id, building has ID ${building.id}")
        logger.error(s"There was an error updating building $id", ex)
        None
    }
  }

  /**
   * Updates the path for the given StoragNodeId
   * @param id the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeId, path: NodePath): Future[MusitResult[Unit]] = {
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 => MusitSuccess(())

      case res: Int =>
        val msg = s"Wrong amount of rows ($res) updated"
        logger.warn(msg)
        MusitInternalError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(mid: MuseumId, building: Building): Future[Building] = {
    val extendedDto = StorageNodeDto.fromBuilding(mid, building)
    val query = for {
      storageUnit <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = storageUnit.id))
      n <- insertAction(extWithId)
    } yield {
      val extNode = ExtendedStorageNode(storageUnit, extWithId)
      StorageNodeDto.toBuilding(extNode)
    }

    db.run(query.transactionally)
  }

  private class BuildingTable(
      val tag: Tag
  ) extends Table[BuildingDto](tag, SchemaName, "BUILDING") {

    def * = (id.?, address) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[StorageNodeId]("STORAGE_NODE_ID", O.PrimaryKey)
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

