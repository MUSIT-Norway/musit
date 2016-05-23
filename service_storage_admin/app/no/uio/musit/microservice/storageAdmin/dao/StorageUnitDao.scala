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


  def getSubNodes(id: Long): Future[Seq[StorageUnit]] = {
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


  def getById(id: Long): Future[Option[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.id === id).result.headOption
    db.run(action)
  }


  private class StorageUnitTable(tag: Tag) extends Table[StorageUnit](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id, storageType,storageUnitName, area, isStorageUnit, isPartOf, height, room_sikringSkallsikring, room_sikringTyverisikring,
      room_sikringBrannsikring, room_sikringVannskaderisiko, room_sikringRutineOgBeredskap,
      room_bevarLuftfuktOgTemp, room_bevarLysforhold, room_bevarPrevantKons, building_address, groupRead, groupWrite) <>(create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def storageType = column[String]("STORAGE_TYPE")

    def storageUnitName = column[Option[String]]("STORAGE_UNIT_NAME")

    def area = column[Option[Long]]("AREA")

    def isStorageUnit = column[Option[String]]("IS_STORAGE_UNIT")

    def isPartOf = column[Option[Long]]("IS_PART_OF")

    def height = column[Option[Long]]("HEIGHT")

    def room_sikringSkallsikring = column[Option[String]]("ROOM_SIKRING_SKALLSIKRING")

    def room_sikringTyverisikring = column[Option[String]]("ROOM_SIKRING_TYVERISIKRING")

    def room_sikringBrannsikring = column[Option[String]]("ROOM_SIKRING_BRANNSIKRING")

    def room_sikringVannskaderisiko = column[Option[String]]("ROOM_SIKRING_VANNSKADERISIKO")

    def room_sikringRutineOgBeredskap = column[Option[String]]("ROOM_SIKRING_RUTINE_OG_BEREDSKAP")

    def room_bevarLuftfuktOgTemp = column[Option[String]]("ROOM_BEVAR_LUFTFUKT_OG_TEMP")

    def room_bevarLysforhold = column[Option[String]]("ROOM_BEVAR_LYSFORHOLD")

    def room_bevarPrevantKons = column[Option[String]]("ROOM_BEVAR_PREVANT_KONS")

    def building_address = column[Option[String]]("BUILDING_POSTAL_ADDRESS")

    def groupRead = column[Option[String]]("GROUP_READ")

    def groupWrite = column[Option[String]]("GROUP_WRITE")

    def create = (id: Long, storageType: String, storageUnitName: Option[String], area: Option[Long], isStorageUnit: Option[String], isPartOf: Option[Long], height: Option[Long],
                  room_sikringSkallsikring: Option[String], room_sikringTyverisikring: Option[String],
                  room_sikringBrannsikring: Option[String], room_sikringVannskaderisiko: Option[String], room_sikringRutineOgBeredskap: Option[String],
                  room_bevarLuftfuktOgTemp: Option[String], room_bevarLysforhold: Option[String], room_bevarPrevantKons: Option[String], building_address: Option[String],
                  groupRead: Option[String], groupWrite: Option[String]) =>
      StorageUnit(
        id, storageType,
        storageUnitName, area, isStorageUnit, isPartOf, height, room_sikringSkallsikring, room_sikringTyverisikring,
        room_sikringBrannsikring, room_sikringVannskaderisiko, room_sikringRutineOgBeredskap,
        room_bevarLuftfuktOgTemp, room_bevarLysforhold, room_bevarPrevantKons, building_address, groupRead, groupWrite,
        Seq(LinkService.self(s"/v1/$id")))

    def destroy(unit: StorageUnit) = Some(unit.id, unit.storageType,
      unit.storageUnitName, unit.area, unit.isStorageUnit, unit.isPartOf, unit.height, unit.room_sikringSkallsikring, unit.room_sikringTyverisikring,
      unit.room_sikringBrannsikring, unit.room_sikringVannskaderisiko, unit.room_sikringRutineOgBeredskap,
      unit.room_bevarLuftfuktOgTemp, unit.room_bevarLysforhold, unit.room_bevarPrevantKons, unit.building_address, unit.groupRead, unit.groupWrite)
  }

}
  /*private class RoomTable(tag: Tag) extends Table[StorageRoom](tag, Some("MUSARK_STORAGE"), "ROOM") {
    def * = (id, sikringSkallsikring, sikringTyverisikring, sikringbrannsikring, sikringVannskaderisiko, sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp, bevarLysforhold, bevarPrevantKons) <>(create.tupled, destroy)

    def id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    def sikringSkallsikring = column[Option[String]]("SIKRING_SKALLSIKRING")

    def sikringTyverisikring = column[Option[String]]("SIKRING_TYVERISIKRING")

    def sikringbrannsikring = column[Option[String]]("SIKRING_BRANNSIKRING")

    def sikringVannskaderisiko = column[Option[String]]("SIKRING_VANNSKADERISIKO")

    def sikringRutineOgBeredskap = column[Option[String]]("SIKRING_RUTINE_OG_BEREDSKAP")

    def bevarLuftfuktOgTemp = column[Option[String]]("BEVAR_LUFTFUKT_OG_TEMP")

    def bevarLysforhold = column[Option[String]]("BEVAR_LYSFORHOLD")

    def bevarPrevantKons = column[Option[String]]("BEVAR_PREVANT_KONS")

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

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Long, address: String) => StorageBuilding(id, address, Seq(LinkService.self(s"/v1/$id")))

    def destroy(building: StorageBuilding) = Some(building.id, building.address)
  }

}
*/