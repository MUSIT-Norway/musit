/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.microservice.storagefacility.dao.storage

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{ExtendedStorageNode, OrganisationDto, StorageNodeDto}
import no.uio.musit.service.MusitResults.{MusitDbError, MusitInternalError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
@Singleton
class OrganisationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[OrganisationDao])

  private val organisationTable = TableQuery[OrganisationTable]

  private def updateAction(id: StorageNodeId, org: OrganisationDto): DBIO[Int] = {
    organisationTable.filter { ot =>
      ot.id === id
    }.update(org)
  }

  private def insertAction(organisationDto: OrganisationDto): DBIO[Int] = {
    organisationTable += organisationDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[Organisation]] = {
    val action = for {
      maybeUnitDto <- getUnitByIdAction(id)
      maybeOrgDto <- organisationTable.filter(_.id === id).result.headOption
    } yield {
      // Map the results into an ExtendedStorageNode type
      maybeUnitDto.flatMap(u =>
        maybeOrgDto.map(o => ExtendedStorageNode(u, o)))
    }
    // Execute the query
    db.run(action).map(_.map { unitOrgTuple =>
      StorageNodeDto.toOrganisation(unitOrgTuple)
    })
  }

  /**
   * TODO: Document me!!!
   */
  def update(
    id: StorageNodeId,
    organisation: Organisation
  ): Future[MusitResult[Option[Int]]] = {
    val extendedOrgDto = StorageNodeDto.fromOrganisation(organisation, Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(id, extendedOrgDto.storageUnitDto)
      orgsUpdated <- updateAction(id, extendedOrgDto.extension)
    } yield orgsUpdated

    db.run(action.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Updates the path for the given StoragNodeId
   * @param id the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeId, path: NodePath): Future[MusitResult[Unit]] = {
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 => MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(organisation: Organisation): Future[StorageNodeId] = {
    val extendedDto = StorageNodeDto.fromOrganisation(organisation)
    val query = for {
      nodeId <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      n <- insertAction(extWithId)
    } yield {
      nodeId
    }

    db.run(query.transactionally)
  }

  private class OrganisationTable(
      val tag: Tag
  ) extends Table[OrganisationDto](tag, SchemaName, "ORGANISATION") {

    def * = (id, address) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[StorageNodeId]]("STORAGE_NODE_ID", O.PrimaryKey)
    val address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[StorageNodeId], address: Option[String]) =>
      OrganisationDto(
        id = id,
        address = address
      )

    def destroy(organisation: OrganisationDto) =
      Some((organisation.id, organisation.address))
  }

}

