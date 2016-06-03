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

  def linkText(id: Option[Long]) = {
    assert(id.isDefined)
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
  }

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageUnit]] = {
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

  def getChildren(id: Long): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  def getStorageType(id: Long): Future[Option[StorageUnitType]] = {
    val action = StorageUnitTable.filter(_.id === id).map {
      _.storageType
    }.result.headOption
    db.run(action).map {
      _.map { storageType => StorageUnitType(storageType)
      }
    }
  }

  def getWholeCollectionStorage(storageCollectionRoot: String): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.storageType === storageCollectionRoot).result
    db.run(action)
  }

  def all(): Future[Seq[StorageUnit]] = db.run(StorageUnitTable.result)

  def insertAndRun(storageUnit: StorageUnit): Future[StorageUnit] =
    db.run(insert(storageUnit))

  def insert(storageUnit: StorageUnit): DBIO[StorageUnit] = {
    val insertQuery = StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) => storageUnit.copy(id = id, links = linkText(id)))
    val action = insertQuery += storageUnit
    action
  }

  def insertRoomOnly(storageRoom: StorageRoom): DBIO[Int] = {
    assert(storageRoom.id.isDefined) //if failed then it's our bug
    val stRoom = storageRoom.copy(links = linkText(storageRoom.id))
    val insertQuery = RoomTable
    val action = insertQuery += stRoom
    action
  }

  def insertRoom(storageUnit: StorageUnit, storageRoom: StorageRoom): Future[(StorageUnit, StorageRoom)] = {
    val action = (for {
      storageUnit <- insert(storageUnit)
      n <- insertRoomOnly(storageRoom.copy(id = storageUnit.id))
    } yield (storageUnit, storageRoom.copy(id = storageUnit.id))).transactionally
    db.run(action)
  }

  def insertBuildingOnly(storageBuilding: StorageBuilding): DBIO[Int] = {
    val stBuilding = storageBuilding.copy(links = linkText(storageBuilding.id))
    val insertQuery = BuildingTable
    val action = insertQuery += stBuilding
    action
  }

  def insertBuilding(storageUnit: StorageUnit, storageBuilding: StorageBuilding): Future[(StorageUnit, StorageBuilding)] = {
    val action = (for {
      storageUnit <- insert(storageUnit)
      n <- insertBuildingOnly(storageBuilding.copy(id = storageUnit.id))
    } yield (storageUnit, storageBuilding.copy(id = storageUnit.id))).transactionally
    db.run(action)
  }

  def updateStorageUnitByIdNoRun(id: Long, storageUnit: StorageUnit): DBIO[Int] = {
    StorageUnitTable.filter(_.id === id).update(storageUnit)
  }

  def updateStorageUnitByID(id: Long, storageUnit: StorageUnit): Future[Int] = {
    db.run(updateStorageUnitByIdNoRun(id, storageUnit))
  }

  def updateRoomOnlyByIdNoRun(id: Long, storageRoom: StorageRoom): DBIO[Int] = {
    RoomTable.filter(_.id === id).update(storageRoom)
  }

  def updateRoomByID(id: Long, storageUnitAndRoom: (StorageUnit, StorageRoom)) = {
    println(s"updateRoomByID: ID: $id  storageRoom: ${storageUnitAndRoom._2}")
    val action = (for {
      n <- updateStorageUnitByIdNoRun(id, storageUnitAndRoom._1)
      m <- updateRoomOnlyByIdNoRun(id, storageUnitAndRoom._2.copy(id = Some(id)))
      if (n == 1 && m == 1)
    } yield 1).transactionally
    db.run(action)
  }

  def updateBuildingOnlyByIdNoRun(id: Long, storageBuilding: StorageBuilding): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  def updateBuildingByID(id: Long, storageUnitAndBuilding: (StorageUnit, StorageBuilding)) = {

    val action = (for {
      n <- updateStorageUnitByIdNoRun(id, storageUnitAndBuilding._1)
      m <- updateBuildingOnlyByIdNoRun(id, storageUnitAndBuilding._2.copy(id = Some(id)))
      if (n == 1 && m == 1)
    } yield 1).transactionally
    db.run(action)
  }

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnit](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id, storageType, storageUnitName, area, isStorageUnit, isPartOf, height, groupRead, groupWrite) <> (create.tupled, destroy)

    val id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[String]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_UNIT_NAME")

    val area = column[Option[Long]]("AREA")

    val isStorageUnit = column[Option[String]]("IS_STORAGE_UNIT")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    def create = (id: Option[Long], storageType: String, storageUnitName: String, area: Option[Long], isStorageUnit: Option[String], isPartOf: Option[Long], height: Option[Long],
      groupRead: Option[String], groupWrite: Option[String]) =>
      StorageUnit(
        id, storageType,
        storageUnitName, area, isStorageUnit, isPartOf, height, groupRead, groupWrite,
        linkText(id)
      )

    def destroy(unit: StorageUnit) = Some(unit.id, unit.storageType,
      unit.storageUnitName, unit.area, unit.isStorageUnit, unit.isPartOf, unit.height, unit.groupRead, unit.groupWrite)
  }

  private class RoomTable(tag: Tag) extends Table[StorageRoom](tag, Some("MUSARK_STORAGE"), "ROOM") {
    def * = (id, sikringSkallsikring, sikringTyverisikring, sikringBrannsikring, sikringVannskaderisiko, sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp, bevarLysforhold, bevarPrevantKons) <> (create.tupled, destroy)

    def id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def sikringSkallsikring = column[Option[String]]("SIKRING_SKALLSIKRING")

    def sikringTyverisikring = column[Option[String]]("SIKRING_TYVERISIKRING")

    def sikringBrannsikring = column[Option[String]]("SIKRING_BRANNSIKRING")

    def sikringVannskaderisiko = column[Option[String]]("SIKRING_VANNSKADERISIKO")

    def sikringRutineOgBeredskap = column[Option[String]]("SIKRING_RUTINE_OG_BEREDSKAP")

    def bevarLuftfuktOgTemp = column[Option[String]]("BEVAR_LUFTFUKT_OG_TEMP")

    def bevarLysforhold = column[Option[String]]("BEVAR_LYSFORHOLD")

    def bevarPrevantKons = column[Option[String]]("BEVAR_PREVANT_KONS")

    def create = (id: Option[Long], sikringSkallsikring: Option[String], sikringTyverisikring: Option[String], sikringBrannsikring: Option[String], sikringVannskaderisiko: Option[String],
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
        linkText(id)
      )

    def destroy(room: StorageRoom) = Some(room.id, room.sikringSkallsikring, room.sikringTyverisikring, room.sikringBrannsikring, room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap, room.bevarLuftfuktOgTemp, room.bevarLysforhold, room.bevarPrevantKons)
  }

  private class BuildingTable(tag: Tag) extends Table[StorageBuilding](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy)

    def id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) => StorageBuilding(id, address, linkText(id))

    def destroy(building: StorageBuilding) = Some(building.id, building.address)
  }

}
