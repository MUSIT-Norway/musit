package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class OrganisationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    storageUnitDao: StorageUnitDao,
    envReqDao: EnvReqDao
) extends HasDatabaseConfigProvider[JdbcProfile] with StorageDtoConverter {

  import driver.api._

  private val OrganisationTable = TableQuery[OrganisationTable]

  def getOrganisationById(id: Long): Future[Option[OrganisationDTO]] = {
    val action = OrganisationTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def updateOrganisationOnlyAction(id: Long, storageOrganisation: OrganisationDTO): DBIO[Int] = {
    OrganisationTable.filter(_.id === id).update(storageOrganisation)
  }

  def updateOrganisation(id: Long, organisation: Organisation) = {
    val organisationDto = organisationToDto(organisation)
    val storageNodePart = organisationDto.storageNode
    val organisationPart = organisationDto.organisationDto
    val updateStorageNodeOnlyAction = storageUnitDao.updateStorageNodeAndMaybeEnvReqAction(id, organisation)
    val combinedAction = updateStorageNodeOnlyAction.flatMap { _ => updateOrganisationOnlyAction(id, organisationPart.copy(id = Some(id))) }
    db.run(combinedAction.transactionally)
  }

  private def insertOrganisationOnlyAction(storageOrganisation: OrganisationDTO): DBIO[Int] = {
    val insertQuery = OrganisationTable
    val action = insertQuery += storageOrganisation
    action
  }

  def insertOrganisation(completeOrganisationDto: CompleteOrganisationDto): Future[CompleteOrganisationDto] = {
    val envReqInsertAction = envReqDao.insertAction(completeOrganisationDto.envReqDto)

    val nodePartIn = completeOrganisationDto.storageNode
    val organisationPartIn = completeOrganisationDto.organisationDto

    val action = (for {
      optEnvReq <- envReqInsertAction
      nodePart = nodePartIn.copy(latestEnvReqId = optEnvReq.map(_.id).flatten)
      nodePartOut <- storageUnitDao.insertAction(nodePart)
      organisationPartOut = organisationPartIn.copy(id = nodePartOut.id)
      n <- insertOrganisationOnlyAction(organisationPartOut)
    } yield CompleteOrganisationDto(nodePartOut, organisationPartOut, optEnvReq)).transactionally
    db.run(action)
  }
  def insertOrganisation(organisation: Organisation): Future[Organisation] = {
    insertOrganisation(organisationToDto(organisation)).map(organisationFromDto)
  }

  private class OrganisationTable(tag: Tag) extends Table[OrganisationDTO](tag, Some("MUSARK_STORAGE"), "ORGANISATION") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_NODE_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) =>
      OrganisationDTO(id, address)

    def destroy(organisation: OrganisationDTO) = Some(organisation.id, organisation.address)
  }

}

