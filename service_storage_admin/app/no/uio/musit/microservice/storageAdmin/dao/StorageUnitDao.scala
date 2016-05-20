package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by ellenjo on 5/18/16.
  */
object StorageUnitDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val StorageUnitTable = TableQuery[StorageUnitTable]
  private val RoomTable = TableQuery[RoomTable]
  private val BuildingTable = TableQuery[BuildingTable]


  def getNodes(id:Long): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  def getWholeCollectionStorage(storageCollectionRoot: String): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.storageType === storageCollectionRoot).result
    db.run(action)
  }

  def all(): Future[Seq[StorageUnit]] = db.run(StorageUnitTable.result)

  def insert(storageUnit: StorageUnit): Future[StorageUnit] = {
    val insertQuery = StorageUnitTable returning StorageUnitTable.map(_.id) into ((storageUnit, id) => storageUnit.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += storageUnit
    db.run(action)
  }

  def insertRoom(room: StorageRoom): Future[StorageRoom] = {
    // if room.id finnes i storage_unit-tabellen så ok, ellers må man også ta insert til storage_unit_tabellen
    val insertQuery = RoomTable returning RoomTable.map(_.id) into ((room, id) => room.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += room
    db.run(action)
  }


  def insertBuilding(building: StorageBuilding): Future[StorageBuilding] = {
    // if building.id finnes i storage_unit-tabellen så ok, ellers må man også ta insert til storage_unit_tabellen
    val insertQuery = BuildingTable returning BuildingTable.map(_.id) into ((building, id) => building.copy(id = id, links = Seq(LinkService.self(s"/v1/$id"))))
    val action = insertQuery += building
    db.run(action)
  }

  /* def getDisplayID(id:Long) :Future[Option[String]] ={
  val action = MusitThingTable.filter( _.id === id).map(_.displayid).result.headOption
  db.run(action)
}*/
  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit) = {
    StorageUnitTable.filter(_.id === id).update(storageUnit)
  }

  def updateStorageNameByID(id: Long, storageName: String) = {
    val u = for {l <- StorageUnitTable if l.id === id
    } yield l.storageUnitName
    u.update(storageName)
  }




  def getById(id: Long): Future[Option[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getRoomById(id: Long): Future[Option[StorageRoom]] = {
    val action = RoomTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getBuildingById(id: Long): Future[Option[StorageBuilding]] = {
    val action = BuildingTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getReadAccess(id: Long, groupRead: String) = {

  }


  private class StorageUnitTable(tag: Tag) extends Table[StorageUnit](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id, storageUnitName, area, isStorageUnit, isPartOf, height, storageType, groupRead, groupWrite) <>(create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def storageUnitName = column[String]("STORAGE_UNIT_NAME")

    def area = column[Long]("AREA")

    def isStorageUnit = column[String]("IS_STORAGE_UNIT")

    def isPartOf = column[Long]("IS_PART_OF")

    def height = column[Long]("HEIGHT")

    def storageType = column[String]("STORAGE_TYPE")

    def groupRead = column[String]("GROUP_READ")

    def groupWrite = column[String]("GROUP_WRITE")

    def create = (id: Long, storageUnitName: String, area: Long, isStorageUnit: String, isPartOf: Long,
                  height: Long, storageType: String, groupRead: String, groupWrite: String) =>
      StorageUnit(
        id,
        storageUnitName,
        area,
        isStorageUnit,
        isPartOf,
        height,
        storageType,
        groupRead,
        groupWrite,
        Seq(LinkService.self(s"/v1/$id")))

    def destroy(unit: StorageUnit) = Some(unit.id, unit.storageUnitName, unit.area, unit.isStorageUnit, unit.isPartOf, unit.height, unit.storageType,
      unit.groupRead, unit.groupWrite)
  }

  private class RoomTable(tag: Tag) extends Table[StorageRoom](tag, Some("MUSARK_STORAGE"), "ROOM") {
    def * = (id, sikringSkallsikring, sikringTyverisikring, sikringbrannsikring, sikringVannskaderisiko, sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp, bevarLysforhold, bevarPrevantKons) <>(create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def sikringSkallsikring = column[String]("SIKRING_SKALLSIKRING")

    def sikringTyverisikring = column[String]("SIKRING_TYVERISIKRING")

    def sikringbrannsikring = column[String]("SIKRING_BRANNSIKRING")

    def sikringVannskaderisiko = column[String]("SIKRING_VANNSKADERISIKO")

    def sikringRutineOgBeredskap = column[String]("SIKRING_RUTINE_OG_BEREDSKAP")

    def bevarLuftfuktOgTemp = column[String]("BEVAR_LUFTFUKT_OG_TEMP")

    def bevarLysforhold = column[String]("BEVAR_LYSFORHOLD")

    def bevarPrevantKons = column[String]("BEVAR_PREVANT_KONS")

    def create = (id: Long, sikringSkallsikring: String, sikringTyverisikring: String, sikringbrannsikring: String, sikringVannskaderisiko: String,
                  sikringRutineOgBeredskap: String, bevarLuftfuktOgTemp: String, bevarLysforhold: String,
                  bevarPrevantKons: String) =>
      StorageRoom(id,
        sikringSkallsikring,
        sikringTyverisikring,
        sikringbrannsikring,
        sikringVannskaderisiko,
        sikringRutineOgBeredskap,
        bevarLuftfuktOgTemp,
        bevarLysforhold,
        bevarPrevantKons,
        Seq(LinkService.self(s"/v1/$id")))

    def destroy(room: StorageRoom) = Some(room.id, room.sikringSkallsikring, room.sikringTyverisikring, room.sikringBrannsikring, room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap, room.bevarLuftfuktOgTemp, room.bevarLysforhold, room.bevarPrevantKons)
  }


  private class BuildingTable(tag: Tag) extends Table[StorageBuilding](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <>(create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def address = column[String]("POSTAL_ADDRESS")

    def create = (id: Long, address: String) => StorageBuilding(id, address, Seq(LinkService.self(s"/v1/$id")))

    def destroy(building: StorageBuilding) = Some(building.id, building.address)
  }

}
