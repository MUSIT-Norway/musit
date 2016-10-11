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
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{BuildingDto, ExtendedStorageNode, StorageNodeDto}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
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

  private def updateAction(id: StorageNodeId, building: BuildingDto): DBIO[Int] = {
    buildingTable.filter(_.id === id).update(building)
  }

  private def insertAction(buildingDto: BuildingDto): DBIO[Int] = {
    buildingTable += buildingDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[Building]] = {
    val action = for {
      maybeUnitDto <- getUnitByIdAction(id)
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
  def update(id: StorageNodeId, building: Building): Future[MusitResult[Option[Int]]] = {
    val extendedBuildingDto = StorageNodeDto.fromBuilding(building, Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(id, extendedBuildingDto.storageUnitDto)
      buildingsUpdated <- updateAction(id, extendedBuildingDto.extension)
    } yield buildingsUpdated

    db.run(action.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"There was an error updating building $id"
        logger.debug(s"Using $id, building has ID ${building.id}")
        logger.error(msg, ex)
        MusitDbError(msg, Some(ex))
    }
  }

  /**
   * Updates the path for the given StoragNodeId
   *
   * @param id   the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeId, path: NodePath): Future[MusitResult[Unit]] = {
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 => MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(building: Building): Future[StorageNodeId] = {
    val extendedDto = StorageNodeDto.fromBuilding(building)
    val query = for {
      nodeId <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      n <- insertAction(extWithId)
    } yield {
      nodeId
    }

    db.run(query.transactionally)
  }

}

