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
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage.dto._
import no.uio.musit.microservice.storagefacility.domain.storage.{Room, StorageNodeId}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitInternalError, MusitResult, MusitSuccess}
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

  private val roomTable = TableQuery[RoomTable]

  private def updateAction(id: StorageNodeId, room: RoomDto): DBIO[Int] = {
    roomTable.filter(_.id === id).update(room)
  }

  private def insertAction(roomDto: RoomDto): DBIO[Int] = {
    roomTable += roomDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[Room]] = {
    val action = for {
      maybeUnitDto <- getUnitByIdAction(id)
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
  def update(id: StorageNodeId, room: Room): Future[MusitResult[Option[Int]]] = {
    val roomDto = StorageNodeDto.fromRoom(room, Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(id, roomDto.storageUnitDto)
      roomsUpdated <- updateAction(id, roomDto.extension)
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
   * @param id the StorageNodeId to update
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
  def insert(room: Room): Future[StorageNodeId] = {
    val extendedDto = StorageNodeDto.fromRoom(room)
    val action = (for {
      nodeId <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      inserted <- insertAction(extWithId)
    } yield {
      nodeId
    }).transactionally

    db.run(action)
  }

  private class RoomTable(
      val tag: Tag
  ) extends Table[RoomDto](tag, SchemaName, "ROOM") {
    // scalastyle:off method.name
    def * = (
      id,
      perimeterSecurity,
      theftProtection,
      fireProtection,
      waterDamage,
      routinesAndContingency,
      relativeHumidity,
      temperatureAssessment,
      lighting,
      preventiveConservation
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Option[StorageNodeId]]("STORAGE_NODE_ID", O.PrimaryKey)
    val perimeterSecurity = column[Option[Boolean]]("PERIMETER_SECURITY")
    val theftProtection = column[Option[Boolean]]("THEFT_PROTECTION")
    val fireProtection = column[Option[Boolean]]("FIRE_PROTECTION")
    val waterDamage = column[Option[Boolean]]("WATER_DAMAGE_ASSESSMENT")
    val routinesAndContingency = column[Option[Boolean]]("ROUTINES_AND_CONTINGENCY_PLAN")
    val relativeHumidity = column[Option[Boolean]]("RELATIVE_HUMIDITY")
    val temperatureAssessment = column[Option[Boolean]]("TEMPERATURE_ASSESSMENT")
    val lighting = column[Option[Boolean]]("LIGHTING_CONDITION")
    val preventiveConservation = column[Option[Boolean]]("PREVENTIVE_CONSERVATION")

    def create = (
      id: Option[StorageNodeId],
      perimeterSecurity: Option[Boolean],
      theftProtection: Option[Boolean],
      fireProtection: Option[Boolean],
      waterDamage: Option[Boolean],
      routinesAndContingency: Option[Boolean],
      relativeHumidity: Option[Boolean],
      temperature: Option[Boolean],
      lighting: Option[Boolean],
      preventiveConservation: Option[Boolean]
    ) =>
      RoomDto(
        id = id,
        perimeterSecurity = perimeterSecurity,
        theftProtection = theftProtection,
        fireProtection = fireProtection,
        waterDamageAssessment = waterDamage,
        routinesAndContingencyPlan = routinesAndContingency,
        relativeHumidity = relativeHumidity,
        temperatureAssessment = temperature,
        lightingCondition = lighting,
        preventiveConservation = preventiveConservation
      )

    def destroy(room: RoomDto) =
      Some((
        room.id,
        room.perimeterSecurity,
        room.theftProtection,
        room.fireProtection,
        room.waterDamageAssessment,
        room.routinesAndContingencyPlan,
        room.relativeHumidity,
        room.temperatureAssessment,
        room.lightingCondition,
        room.preventiveConservation
      ))
  }

}
