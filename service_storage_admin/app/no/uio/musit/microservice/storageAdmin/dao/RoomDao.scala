package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.DaoHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile
import no.uio.musit.microservices.common.utils.Misc._
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RoomDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

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
    val updateStorageUnitOnlyAction = StorageUnitDao.updateStorageUnitAction(id, Storage.toDTO(room))

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
      storageUnit <- StorageUnitDao.insertAction(storageUnit)
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

    def create = (id: Option[Long], sikringSkallsikring: Option[Boolean], sikringTyverisikring: Option[Boolean],
      sikringBrannsikring: Option[Boolean], sikringVannskaderisiko: Option[Boolean],
      sikringRutineOgBeredskap: Option[Boolean], bevarLuftfuktOgTemp: Option[Boolean],
      bevarLysforhold: Option[Boolean], bevarPrevantKons: Option[Boolean]) =>
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
