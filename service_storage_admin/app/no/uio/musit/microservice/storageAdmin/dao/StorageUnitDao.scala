package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.utils.{ DaoHelper, ErrorHelper }
import no.uio.musit.microservices.common.utils.Misc._
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
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

  def linkText(id: Option[Long]) = {
    assert(id.isDefined)
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
  }

  def unknownStorageUnitMsg(id: Long) = s"Unknown storageUnit with id: $id"

  def storageUnitNotFoundError(id: Long): MusitError = {
    ErrorHelper.notFound(unknownStorageUnitMsg(id))
  }

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageUnit]] = {
    val action = StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).result.headOption
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

  //def getStorageType(id: Long): Future[Option[StorageUnitType]] = {
  def getStorageType(id: Long): MusitFuture[StorageUnitType] = {
    val action = StorageUnitTable.filter(_.id === id).map {
      _.storageType
    }.result.headOption
    val res = db.run(action)
    res.foldInnerOption(Left(storageUnitNotFoundError(id)), storageType => StorageUnitType(storageType) match {
      case Some(st) => Right(st)
      case _ => Left(ErrorHelper.conflict("Illegal storageType."))
    })
  }

  def getWholeCollectionStorage(storageCollectionRoot: String): Future[Seq[StorageUnit]] = {
    val action = StorageUnitTable.filter(_.storageType === storageCollectionRoot).result
    db.run(action)
  }

  def all(): Future[Seq[StorageUnit]] = db.run(StorageUnitTable.result)

  def insert(storageUnit: StorageUnit): Future[StorageUnit] =
    db.run(insertAction(storageUnit))

  def insertAction(storageUnit: StorageUnit): DBIO[StorageUnit] = {
    val insertQuery = StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) => storageUnit.copy(id = id, links = linkText(id)))
    val action = insertQuery += storageUnit
    action
  }

  private def insertRoomOnlyAction(storageRoom: StorageRoom): DBIO[Int] = {
    assert(storageRoom.id.isDefined) //if failed then it's our bug
    val stRoom = storageRoom.copy(links = linkText(storageRoom.id))
    val insertQuery = RoomTable
    val action = insertQuery += stRoom
    action
  }

  def insertRoom(storageUnit: StorageUnit, storageRoom: StorageRoom): Future[StorageUnitTriple] = {
    val action = (for {
      storageUnit <- insertAction(storageUnit)
      n <- insertRoomOnlyAction(storageRoom.copy(id = storageUnit.id))
    } yield StorageUnitTriple.createRoom(storageUnit, storageRoom.copy(id = storageUnit.id))).transactionally
    db.run(action)
  }

  private def insertBuildingOnlyAction(storageBuilding: StorageBuilding): DBIO[Int] = {
    val stBuilding = storageBuilding.copy(links = linkText(storageBuilding.id))
    val insertQuery = BuildingTable
    val action = insertQuery += stBuilding
    action
  }

  def insertBuilding(storageUnit: StorageUnit, storageBuilding: StorageBuilding): Future[StorageUnitTriple] = {
    val action = (for {
      storageUnit <- insertAction(storageUnit)
      n <- insertBuildingOnlyAction(storageBuilding.copy(id = storageUnit.id))
    } yield StorageUnitTriple.createBuilding(storageUnit, storageBuilding.copy(id = storageUnit.id))).transactionally
    db.run(action)
  }

  private def updateStorageUnitAction(id: Long, storageUnit: StorageUnit): DBIO[Int] = {
    StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  def updateStorageUnit(id: Long, storageUnit: StorageUnit): Future[Int] = {
    db.run(updateStorageUnitAction(id, storageUnit))
  }

  private def updateRoomOnlyAction(id: Long, storageRoom: StorageRoom): DBIO[Int] = {
    RoomTable.filter(_.id === id).update(storageRoom)
  }

  def updateRoom(id: Long, storageUnitAndRoom: (StorageUnit, StorageRoom)) = {

    //If we don't have the storage unit or it is marked as deleted, or we find more than 1 rows to update, onlyAcceptOneUpdatedRecord
    // will make this DBIO/Future fail with an appropriate MusitException.
    // (Which later gets recovered in ServiceHelper.daoUpdate)
    val updateStorageUnitOnlyAction = updateStorageUnitAction(id, storageUnitAndRoom._1) |> DaoHelper.onlyAcceptOneUpdatedRecord

    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateRoomOnlyAction(id, storageUnitAndRoom._2.copy(id = Some(id))) }

    db.run(combinedAction.transactionally)
  }

  /* # Previous version:
  def updateRoom(id: Long, storageUnitAndRoom: (StorageUnit, StorageRoom)) = {
      val action = (for {
      n <- updateStorageUnitAction(id, storageUnitAndRoom._1)
      m <- updateRoomOnlyAction(id, storageUnitAndRoom._2.copy(id = Some(id)))
      if (n == 1 && m == 1)
    } yield 1).transactionally
    db.run(action)
}
   */

  private def updateBuildingOnlyAction(id: Long, storageBuilding: StorageBuilding): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  /** @see #updateRoom() */
  def updateBuilding(id: Long, storageUnitAndBuilding: (StorageUnit, StorageBuilding)) = {
    val updateStorageUnitOnlyAction = updateStorageUnitAction(id, storageUnitAndBuilding._1) |> DaoHelper.onlyAcceptOneUpdatedRecord

    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateBuildingOnlyAction(id, storageUnitAndBuilding._2.copy(id = Some(id))) }

    db.run(combinedAction.transactionally)
  }

  def deleteStorageUnit(id: Long): Future[Int] = {
    db.run((for {
      storageUnit <- StorageUnitTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  case class StorageUnitStuff(id: Long, storageType: String, storageUnitName: String, area: Option[Long], areaTo: Option[Long],)

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnit](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[String]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_UNIT_NAME")

    val area = column[Option[Long]]("AREA")

    val areaTo = column[Option[Long]]("AREA_TO")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val heightTo = column[Option[Long]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val isDeleted = column[Boolean]("IS_DELETED")

    def create = (id: Option[Long], storageType: String, storageUnitName: String, area: Option[Long], areaTo: Option[Long],
      isPartOf: Option[Long], height: Option[Long], heightTo: Option[Long],
      groupRead: Option[String], groupWrite: Option[String], isDeleted: Boolean) =>
      StorageUnit(
        id,
        storageType,
        storageUnitName,
        area,
        areaTo,
        isPartOf,
        height,
        heightTo,
        groupRead,
        groupWrite,
        linkText(id)
      ).delete(isDeleted)

    def destroy(unit: StorageUnit) = Some(unit.id, unit.storageType,
      unit.storageUnitName, unit.area, unit.areaTo, unit.isPartOf, unit.height, unit.heightTo, unit.groupRead, unit.groupWrite, unit.isDeleted)
  }

  private class RoomTable(tag: Tag) extends Table[StorageRoom](tag, Some("MUSARK_STORAGE"), "ROOM") {

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

    def destroy(room: StorageRoom) = Some(room.id, room.sikringSkallsikring, room.sikringTyverisikring,
      room.sikringBrannsikring, room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap, room.bevarLuftfuktOgTemp, room.bevarLysforhold, room.bevarPrevantKons)
  }

  private class BuildingTable(tag: Tag) extends Table[StorageBuilding](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) => StorageBuilding(id, address, linkText(id))

    def destroy(building: StorageBuilding) = Some(building.id, building.address)
  }

}
