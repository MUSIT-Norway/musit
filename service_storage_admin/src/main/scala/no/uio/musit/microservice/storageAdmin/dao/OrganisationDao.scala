package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageNodeDTO
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class OrganisationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    storageUnitDao: StorageUnitDao
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val OrganisationTable = TableQuery[OrganisationTable]

  def getOrganisationById(id: Long): Future[Option[Organisation]] = {
    val action = OrganisationTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  def updateOrganisationOnlyAction(id: Long, storageOrganisation: Organisation): DBIO[Int] = {
    OrganisationTable.filter(_.id === id).update(storageOrganisation)
  }

  def updateOrganisation(id: Long, organisation: Organisation) = {
    val updateStorageUnitOnlyAction = storageUnitDao.updateStorageUnitAction(id, Storage.toDTO(organisation))
    val combinedAction = updateStorageUnitOnlyAction.flatMap { _ => updateOrganisationOnlyAction(id, organisation.copy(id = Some(id))) }
    db.run(combinedAction.transactionally)
  }

  private def insertOrganisationOnlyAction(storageOrganisation: Organisation): DBIO[Int] = {
    val stOrganisation = storageOrganisation.copy(links = Storage.linkText(storageOrganisation.id))
    val insertQuery = OrganisationTable
    val action = insertQuery += stOrganisation
    action
  }

  def insertOrganisation(storageUnit: StorageNodeDTO, storageOrganisation: Organisation): Future[Storage] = {
    val action = (for {
      storageUnit <- storageUnitDao.insertAction(storageUnit)
      n <- insertOrganisationOnlyAction(storageOrganisation.copy(id = storageUnit.id))
    } yield Storage.getOrganisation(storageUnit, storageOrganisation)).transactionally
    db.run(action)
  }

  private class OrganisationTable(tag: Tag) extends Table[Organisation](tag, Some("MUSARK_STORAGE"), "ORGANISATION") {
    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    def id = column[Option[Long]]("STORAGE_NODE_ID", O.PrimaryKey)

    def address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[Long], address: Option[String]) =>
      Organisation(id, null, None, None, None, None, None, None, None, None, None, None, address)

    def destroy(organisation: Organisation) = Some(organisation.id, organisation.address)
  }

}

