package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class BuildingDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val BuildingTable = TableQuery[BuildingTable]

  def getBuildingById(id: Long): Future[Option[Building]] = {
    val action = BuildingTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def updateBuildingOnlyAction(id: Long, storageBuilding: Building): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  def updateBuilding(id: Long, building: Building) = {
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageUnitAction(id, Storage.toDTO(building))
    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateBuildingOnlyAction(id, building.copy(id = Some(id))) }
    db.run(combinedAction.transactionally)
  }

  private def insertBuildingOnlyAction(storageBuilding: Building): DBIO[Int] = {
    val stBuilding = storageBuilding.copy(links = Storage.linkText(storageBuilding.id))
    val insertQuery = BuildingTable
    val action = insertQuery += stBuilding
    action
  }

  def insertBuilding(storageUnit: StorageNodeDTO, storageBuilding: Building): Future[Storage] = {
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(storageUnit)
      n <- insertBuildingOnlyAction(storageBuilding.copy(id = storageUnit.id))
    } yield Storage.getBuilding(storageUnit, storageBuilding)).transactionally
    db.run(action)
  }

  private class BuildingTable(tag: Tag) extends Table[Building](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_NODE_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) =>
      Building(id, null, None, None, None, None, None, None, None, None, None, None, address)

    def destroy(building: Building) = Some(building.id, building.address)
  }

}

