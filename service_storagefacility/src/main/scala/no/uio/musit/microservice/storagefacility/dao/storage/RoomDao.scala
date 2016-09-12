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
import no.uio.musit.microservice.storagefacility.domain.storage.dto._
import no.uio.musit.microservice.storagefacility.domain.storage.{ Room, StorageNodeId }
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class RoomDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[RoomDao])

  private val roomTable = TableQuery[RoomTable]

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[Room]] = {
    val action = for {
      maybeUnitDto <- storageUnitDao.getByIdAction(id)
      maybeRoomDto <- roomTable.filter(_.id === id).result.headOption
    } yield {
      maybeUnitDto.flatMap(u =>
        maybeRoomDto.map(r => ExtendedStorageNode(u, r)))
    }
    db.run(action).map(_.map { unitRoomTuple =>
      StorageNodeDto.toRoom(unitRoomTuple)
    })
  }

  private[dao] def updateAction(id: StorageNodeId, room: RoomDto): DBIO[Int] = {
    roomTable.filter(_.id === id).update(room)
  }

  /**
   * TODO: Document me!!!
   */
  def update(id: Long, room: Room): Future[Option[Room]] = {
    // FIXME: Update comments
    //If we don't have the storage unit or it is marked as deleted, or we find
    // more than 1 rows to update, onlyAcceptOneUpdatedRecord will make this
    // DBIO/Future fail with an appropriate MusitException.
    val roomDto = StorageNodeDto.fromRoom(room)
    val action = for {
      unitsUpdated <- storageUnitDao.updateAction(id, roomDto.storageUnitDto)
      roomsUpdated <- updateAction(id, roomDto.extension.copy(id = Some(id)))
    } yield roomsUpdated

    db.run(action.transactionally).flatMap {
      case res: Int if res > 1 || res < 0 =>
        logger.warn("Wrong amount of rows updated")
        Future.successful(None)

      case res: Int =>
        getById(id)
    }
  }

  protected[dao] def insertAction(roomDto: RoomDto): DBIO[RoomDto] = {
    roomTable returning roomTable
      .map(_.id) into ((room, id) => room.copy(id = id)) += roomDto
  }

  /**
   * TODO: Document me!!!
   */
  def insert(room: Room): Future[Room] = {
    val extendedDto = StorageNodeDto.fromRoom(room)
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(extendedDto.storageUnitDto)
      inserted <- insertAction(extendedDto.extension.copy(id = storageUnit.id))
    } yield {
      val extNode = ExtendedStorageNode(storageUnit, inserted)
      StorageNodeDto.toRoom(extNode)
    }).transactionally

    db.run(action)
  }

  private class RoomTable(
      val tag: Tag
  ) extends Table[RoomDto](tag, SchemaName, "ROOM") {
    // scalastyle:off method.name
    def * = (
      id,
      sikringSkallsikring,
      sikringTyverisikring,
      sikringBrannsikring,
      sikringVannskaderisiko,
      sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp,
      bevarLysforhold,
      bevarPrevantKons
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Option[StorageNodeId]]("STORAGE_UNIT_ID", O.PrimaryKey)
    val sikringSkallsikring = column[Option[Boolean]]("SIKRING_SKALLSIKRING")
    val sikringTyverisikring = column[Option[Boolean]]("SIKRING_TYVERISIKRING")
    val sikringBrannsikring = column[Option[Boolean]]("SIKRING_BRANNSIKRING")
    val sikringVannskaderisiko = column[Option[Boolean]]("SIKRING_VANNSKADERISIKO")
    val sikringRutineOgBeredskap = column[Option[Boolean]]("SIKRING_RUTINE_OG_BEREDSKAP")
    val bevarLuftfuktOgTemp = column[Option[Boolean]]("BEVAR_LUFTFUKT_OG_TEMP")
    val bevarLysforhold = column[Option[Boolean]]("BEVAR_LYSFORHOLD")
    val bevarPrevantKons = column[Option[Boolean]]("BEVAR_PREVANT_KONS")

    def create = (
      id: Option[StorageNodeId],
      sikringSkallsikring: Option[Boolean],
      sikringTyverisikring: Option[Boolean],
      sikringBrannsikring: Option[Boolean],
      sikringVannskaderisiko: Option[Boolean],
      sikringRutineOgBeredskap: Option[Boolean],
      bevarLuftfuktOgTemp: Option[Boolean],
      bevarLysforhold: Option[Boolean],
      bevarPrevantKons: Option[Boolean]
    ) =>
      RoomDto(
        id = id,
        sikringSkallsikring = sikringSkallsikring,
        sikringTyverisikring = sikringTyverisikring,
        sikringBrannsikring = sikringBrannsikring,
        sikringVannskaderisiko = sikringVannskaderisiko,
        sikringRutineOgBeredskap = sikringRutineOgBeredskap,
        bevarLuftfuktOgTemp = bevarLuftfuktOgTemp,
        bevarLysforhold = bevarLysforhold,
        bevarPrevantKons = bevarPrevantKons
      )

    def destroy(room: RoomDto) =
      Some((
        room.id,
        room.sikringSkallsikring,
        room.sikringTyverisikring,
        room.sikringBrannsikring,
        room.sikringVannskaderisiko,
        room.sikringRutineOgBeredskap,
        room.bevarLuftfuktOgTemp,
        room.bevarLysforhold,
        room.bevarPrevantKons
      ))
  }

}
