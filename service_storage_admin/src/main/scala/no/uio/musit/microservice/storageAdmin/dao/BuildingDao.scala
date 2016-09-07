package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.{ BuildingDTO, CompleteBuildingDto, StorageNodeDTO }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageDtoConverter

@Singleton
class BuildingDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] with StorageDtoConverter {

  import driver.api._

  private val BuildingTable = TableQuery[BuildingTable]

  def getBuildingById(id: Long): Future[Option[BuildingDTO]] = {
    val action = BuildingTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def updateBuildingOnlyAction(id: Long, storageBuilding: BuildingDTO): DBIO[Int] = {
    BuildingTable.filter(_.id === id).update(storageBuilding)
  }

  def updateBuilding(id: Long, building: Building) = {
    val buildingDto = buildingToDto(building)
    val storageNodePart = buildingDto.storageNode
    val buildingPart = buildingDto.buildingDto

    val updateStorageNodeOnlyAction = storageUnitDao.updateNodeUnitAction(id, storageNodePart)
    val combinedAction = updateStorageNodeOnlyAction.flatMap { _ => updateBuildingOnlyAction(id, buildingPart.copy(id = Some(id))) }
    db.run(combinedAction.transactionally)
  }

  private def insertBuildingOnlyAction(storageBuilding: BuildingDTO): DBIO[Int] = {
    val insertQuery = BuildingTable
    val action = insertQuery += storageBuilding
    action
  }

  def insertBuilding(completeBuildingDto: CompleteBuildingDto): Future[Storage] = {
    val nodePartIn = completeBuildingDto.storageNode
    val buildingPartIn = completeBuildingDto.buildingDto

    val action = (for {
      nodePartOut <- storageUnitDao.insertAction(nodePartIn)
      buildingPartOut = buildingPartIn.copy(id = nodePartOut.id)
      n <- insertBuildingOnlyAction(buildingPartOut)
    } yield buildingFromDto(CompleteBuildingDto(nodePartOut, buildingPartOut))).transactionally
    db.run(action)
  }

  private class BuildingTable(tag: Tag) extends Table[BuildingDTO](tag, Some("MUSARK_STORAGE"), "BUILDING") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_NODE_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) =>
      BuildingDTO(id, address)

    def destroy(building: BuildingDTO) = Some(building.id, building.address)
  }

}

