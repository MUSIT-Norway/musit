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

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{ ExtendedStorageNode, OrganisationDto, StorageNodeDto }
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
      maybeUnitDto <- getNodeByIdAction(id)
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
  def update(id: StorageNodeId, organisation: Organisation): Future[Option[Organisation]] = {
    val extendedOrgDto = StorageNodeDto.fromOrganisation(organisation)
    val action = for {
      unitsUpdated <- updateNodeAction(id, extendedOrgDto.storageUnitDto)
      orgsUpdated <- updateAction(id, extendedOrgDto.extension.copy(id = Some(id)))
    } yield orgsUpdated

    db.run(action.transactionally).flatMap {
      case res: Int if res == 1 =>
        getById(id)

      case res: Int =>
        logger.warn(s"Wrong amount of rows ($res) updated")
        Future.successful(None)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(organisation: Organisation): Future[Organisation] = {
    val extendedDto = StorageNodeDto.fromOrganisation(organisation)
    val query = for {
      storageUnit <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = storageUnit.id))
      n <- insertAction(extWithId)
    } yield {
      val extNode = ExtendedStorageNode(storageUnit, extWithId)
      StorageNodeDto.toOrganisation(extNode)
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

