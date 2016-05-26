package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by ellenjo on 5/18/16.
 */
object StorageUnitDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val StorageUnitTable = TableQuery[StorageUnitTable]
  private val RoomTable = TableQuery[RoomTable]
  private val BuildingTable = TableQuery[BuildingTable]

  def getById(id: Long): Future[Option[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  def getWholeCollectionStorage(storageCollectionRoot: String): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.storageType === storageCollectionRoot).result
    db.run(action)
  }

  def all(): Future[Seq[StorageUnit]] = db.run(StorageUnitTable.result)

  def insert(storageUnit: StorageUnit): Future[StorageUnit] = {
    val insertQuery = StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) => storageUnit.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += storageUnit
    db.run(action)

  }

  def insertRoomOnly(storageRoom: StorageRoom): Future[StorageRoom] = {
    val insertQuery = RoomTable returning RoomTable.map(_.id) into ((storageRoom, id) => storageRoom.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += storageRoom
    db.run(action)
  }

  def insertRoom(storageUnit: StorageUnit, storageRoom: StorageRoom): Future[(StorageUnit, StorageRoom)] = {
    for {
      storageUnitVal <- insert(storageUnit)
      roomVal <- insertRoomOnly(storageRoom)
    } yield (storageUnitVal, roomVal)
  }

  def insertBuildingOnly(storageBuilding: StorageBuilding): Future[StorageBuilding] = {
    val insertQuery = BuildingTable returning BuildingTable.map(_.id) into
      ((storageBuilding, id) => storageBuilding.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += storageBuilding
    db.run(action)
  }

  def insertBuilding(storageUnit: StorageUnit, storageBuilding: StorageBuilding): Future[(StorageUnit, StorageBuilding)] = {
    for {
      storageUnitVal <- insert(storageUnit)
      buildingVal <- insertBuildingOnly(storageBuilding)
    } yield (storageUnitVal, buildingVal)
  }

  /* def getDisplayID(id:Long) :Future[Option[String]] ={
  val action = MusitThingTable.filter( _.id === id).map(_.displayid).result.headOption
  db.run(action)
}*/
  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) = {
    StorageUnitTable.filter(_.id === id).update(storageUnit)
  }

  /*def updateStorageNameByID(id: Long, storageName: String) = {
    val u = for {l <- StorageUnitTable if l.id === id
    } yield l.storageUnitName
    u.update(storageName)
  }*/

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnit](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id, storageType, storageUnitName, area, isStorageUnit, isPartOf, height, groupRead, groupWrite) <> (create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def storageType = column[String]("STORAGE_TYPE")

    def storageUnitName = column[String]("STORAGE_UNIT_NAME")

    def area = column[Option[Long]]("AREA")

    def isStorageUnit = column[Option[String]]("IS_STORAGE_UNIT")

    def isPartOf = column[Option[Long]]("IS_PART_OF")

    def height = column[Option[Long]]("HEIGHT")

    def groupRead = column[Option[String]]("GROUP_READ")

    def groupWrite = column[Option[String]]("GROUP_WRITE")

    def create = (id: Long, storageType: String, storageUnitName: String, area: Option[Long], isStorageUnit: Option[String], isPartOf: Option[Long], height: Option[Long],
      groupRead: Option[String], groupWrite: Option[String]) =>
      StorageUnit(
        id, storageType,
        storageUnitName, area, isStorageUnit, isPartOf, height, groupRead, groupWrite,
        Seq(LinkService.self(s"/v1/$id"))
      )

    def destroy(unit: StorageUnit) = Some(unit.id, unit.storageType,
      unit.storageUnitName, unit.area, unit.isStorageUnit, unit.isPartOf, unit.height, unit.groupRead, unit.groupWrite)
  }

  private class RoomTable(tag: Tag) extends Table[StorageRoom](tag, Some("MUSARK_STORAGE"), "ROOM") {
    def * = (id, sikringSkallsikring, sikringTyverisikring, sikringBrannsikring, sikringVannskaderisiko, sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp, bevarLysforhold, bevarPrevantKons) <> (create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def sikringSkallsikring = column[Option[String]]("SIKRING_SKALLSIKRING")

    def sikringTyverisikring = column[Option[String]]("SIKRING_TYVERISIKRING")

    def sikringBrannsikring = column[Option[String]]("SIKRING_BRANNSIKRING")

    def sikringVannskaderisiko = column[Option[String]]("SIKRING_VANNSKADERISIKO")

    def sikringRutineOgBeredskap = column[Option[String]]("SIKRING_RUTINE_OG_BEREDSKAP")

    def bevarLuftfuktOgTemp = column[Option[String]]("BEVAR_LUFTFUKT_OG_TEMP")

    def bevarLysforhold = column[Option[String]]("BEVAR_LYSFORHOLD")

    def bevarPrevantKons = column[Option[String]]("BEVAR_PREVANT_KONS")

    def create = (id: Long, sikringSkallsikring: Option[String], sikringTyverisikring: Option[String], sikringBrannsikring: Option[String], sikringVannskaderisiko: Option[String],
      sikringRutineOgBeredskap: Option[String], bevarLuftfuktOgTemp: Option[String], bevarLysforhold: Option[String],
      bevarPrevantKons: Option[String]) =>
      StorageRoom(
        id,
        sikringSkallsikring,
        sikringTyverisikring,
        sikringBrannsikring,
        sikringVannskaderisiko,
        sikringRutineOgBeredskap,
        bevarLuftfuktOgTemp,
        bevarLysforhold,
        bevarPrevantKons,
        Seq(LinkService.self(s"/v1/$id"))
      )

    def destroy(room: StorageRoom) = Some(room.id, room.sikringSkallsikring, room.sikringTyverisikring, room.sikringBrannsikring, room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap, room.bevarLuftfuktOgTemp, room.bevarLysforhold, room.bevarPrevantKons)
  }

  private class BuildingTable(tag: Tag) extends Table[StorageBuilding](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Long, address: Option[String]) => StorageBuilding(id, address, Seq(LinkService.self(s"/v1/$id")))

    def destroy(building: StorageBuilding) = Some(building.id, building.address)
  }

}
