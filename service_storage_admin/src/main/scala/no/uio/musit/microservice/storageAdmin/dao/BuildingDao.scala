package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BuildingDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val BuildingTable = TableQuery[BuildingTable]

  def getBuildingById(id: Long): Future[Option[Building]] = {
    val action = BuildingTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def updateBuildingOnlyAction(id: Long, storageBuilding: Building): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  def updateBuilding(id: Long, building: Building) = {
    val updateStorageUnitOnlyAction = StorageUnitDao.updateStorageUnitAction(id, Storage.toDTO(building))
    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateBuildingOnlyAction(id, building.copy(id = Some(id))) }
    db.run(combinedAction.transactionally)
  }

  private def insertBuildingOnlyAction(storageBuilding: Building): DBIO[Int] = {
    val stBuilding = storageBuilding.copy(links = Storage.linkText(storageBuilding.id))
    val insertQuery = BuildingTable
    val action = insertQuery += stBuilding
    action
  }

  def insertBuilding(storageUnit: StorageUnitDTO, storageBuilding: Building): Future[Storage] = {
    val action = (for {
      storageUnit <- StorageUnitDao.insertAction(storageUnit)
      n <- insertBuildingOnlyAction(storageBuilding.copy(id = storageUnit.id))
    } yield Storage.getBuilding(storageUnit, storageBuilding)).transactionally
    db.run(action)
  }

  private class BuildingTable(tag: Tag) extends Table[Building](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_UNIT_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) =>
      Building(id, null, None, None, None, None, None, None, None, None,
        address)

    def destroy(building: Building) = Some(building.id, building.address)
  }
}

