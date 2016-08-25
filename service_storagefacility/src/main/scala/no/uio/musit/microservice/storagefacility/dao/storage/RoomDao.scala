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
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDTO
import no.uio.musit.microservice.storagefacility.domain.storage.{ Room, Storage }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class RoomDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val RoomTable = TableQuery[RoomTable]

  def getRoomById(id: Long): Future[Option[Room]] = {
    val action = RoomTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  private def updateRoomOnlyAction(id: Long, storageRoom: Room): DBIO[Int] = {
    RoomTable.filter(_.id === id).update(storageRoom)
  }

  def updateRoom(id: Long, room: Room) = {

    //If we don't have the storage unit or it is marked as deleted, or we find more than 1 rows to update, onlyAcceptOneUpdatedRecord
    // will make this DBIO/Future fail with an appropriate MusitException.
    // (Which later gets recovered in ServiceHelper.daoUpdate)
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageUnitAction(id, Storage.toDTO(room))

    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateRoomOnlyAction(id, room.copy(id = Some(id))) }

    db.run(combinedAction.transactionally)
  }

  private def insertRoomOnlyAction(storageRoom: Room): DBIO[Int] = {
    assert(storageRoom.id.isDefined) //if failed then it's our bug
    val stRoom = storageRoom.copy(links = Storage.linkText(storageRoom.id))
    val insertQuery = RoomTable
    val action = insertQuery += stRoom
    action
  }

  def insertRoom(storageUnit: StorageUnitDTO, storageRoom: Room): Future[Storage] = {
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(storageUnit)
      n <- insertRoomOnlyAction(storageRoom.copy(id = storageUnit.id))
    } yield Storage.getRoom(storageUnit, storageRoom)).transactionally
    db.run(action)
  }

  private class RoomTable(tag: Tag) extends Table[Room](tag, Some("MUSARK_STORAGE"), "ROOM") {

    def * = (id, sikringSkallsikring, sikringTyverisikring, sikringBrannsikring, sikringVannskaderisiko, // scalastyle:ignore
      sikringRutineOgBeredskap, bevarLuftfuktOgTemp, bevarLysforhold, bevarPrevantKons) <> (create.tupled, destroy)

    def id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def sikringSkallsikring = column[Option[Boolean]]("SIKRING_SKALLSIKRING")

    def sikringTyverisikring = column[Option[Boolean]]("SIKRING_TYVERISIKRING")

    def sikringBrannsikring = column[Option[Boolean]]("SIKRING_BRANNSIKRING")

    def sikringVannskaderisiko = column[Option[Boolean]]("SIKRING_VANNSKADERISIKO")

    def sikringRutineOgBeredskap = column[Option[Boolean]]("SIKRING_RUTINE_OG_BEREDSKAP")

    def bevarLuftfuktOgTemp = column[Option[Boolean]]("BEVAR_LUFTFUKT_OG_TEMP")

    def bevarLysforhold = column[Option[Boolean]]("BEVAR_LYSFORHOLD")

    def bevarPrevantKons = column[Option[Boolean]]("BEVAR_PREVANT_KONS")

    def create = (
      id: Option[Long], sikringSkallsikring: Option[Boolean], sikringTyverisikring: Option[Boolean],
      sikringBrannsikring: Option[Boolean], sikringVannskaderisiko: Option[Boolean],
      sikringRutineOgBeredskap: Option[Boolean], bevarLuftfuktOgTemp: Option[Boolean],
      bevarLysforhold: Option[Boolean], bevarPrevantKons: Option[Boolean]
    ) =>
      Room(id, null, None, None, None, None, None, None, None, None,
        sikringSkallsikring,
        sikringTyverisikring,
        sikringBrannsikring,
        sikringVannskaderisiko,
        sikringRutineOgBeredskap,
        bevarLuftfuktOgTemp,
        bevarLysforhold,
        bevarPrevantKons)

    def destroy(room: Room) = Some(room.id, room.sikringSkallsikring, room.sikringTyverisikring,
      room.sikringBrannsikring, room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap, room.bevarLuftfuktOgTemp, room.bevarLysforhold, room.bevarPrevantKons)
  }

}
