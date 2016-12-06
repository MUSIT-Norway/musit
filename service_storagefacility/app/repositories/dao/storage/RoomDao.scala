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

package repositories.dao.storage

import com.google.inject.{Inject, Singleton}
import models.storage.Room
import models.storage.dto.{ExtendedStorageNode, RoomDto, StorageNodeDto}
import no.uio.musit.models.{MuseumId, NodePath, StorageNodeDatabaseId}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class RoomDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[RoomDao])

  private def updateAction(id: StorageNodeDatabaseId, room: RoomDto): DBIO[Int] = {
    roomTable.filter(_.id === id).update(room)
  }

  private def insertAction(roomDto: RoomDto): DBIO[Int] = {
    roomTable += roomDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(mid: MuseumId, id: StorageNodeDatabaseId): Future[Option[Room]] = {
    val action = for {
      maybeUnitDto <- getUnitByIdAction(mid, id)
      maybeRoomDto <- roomTable.filter(_.id === id).result.headOption
    } yield {
      maybeUnitDto.flatMap(u =>
        maybeRoomDto.map(r => ExtendedStorageNode(u, r)))
    }
    db.run(action).map(_.map { unitRoomTuple =>
      StorageNodeDto.toRoom(unitRoomTuple)
    })
  }

  /**
   * TODO: Document me!!!
   */
  def update(
    mid: MuseumId,
    id: StorageNodeDatabaseId,
    room: Room
  ): Future[MusitResult[Option[Int]]] = {
    val roomDto = StorageNodeDto.fromRoom(mid, room, Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(mid, id, roomDto.storageUnitDto)
      roomsUpdated <- if (unitsUpdated > 0) updateAction(id, roomDto.extension) else DBIO.successful[Int](0) // scalastyle:ignore
    } yield roomsUpdated

    db.run(action.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Set the path for the given StoragNodeId
   *
   * @param id   the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeDatabaseId, path: NodePath): Future[MusitResult[Unit]] = {
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
  def insert(mid: MuseumId, room: Room): Future[StorageNodeDatabaseId] = {
    val extendedDto = StorageNodeDto.fromRoom(mid, room)
    val action = (for {
      nodeId <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      inserted <- insertAction(extWithId)
    } yield {
      nodeId
    }).transactionally

    db.run(action)
  }

}
