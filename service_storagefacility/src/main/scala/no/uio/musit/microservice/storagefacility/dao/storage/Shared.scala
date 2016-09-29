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

import no.uio.musit.microservice.storagefacility.dao._
import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.storage.{ StorageNodeId, StorageType }
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

private[dao] trait BaseStorageDao extends HasDatabaseConfigProvider[JdbcProfile]

private[dao] trait SharedStorageTables extends BaseStorageDao
    with ColumnTypeMappers {

  import driver.api._

  protected val rootNodeType: StorageType = StorageType.RootType

  protected val storageNodeTable = TableQuery[StorageNodeTable]

  protected[dao] def getUnitByIdAction(id: StorageNodeId): DBIO[Option[StorageUnitDto]] = {
    storageNodeTable.filter { st =>
      st.id === id && st.isDeleted === false && st.storageType =!= rootNodeType
    }.result.headOption
  }

  protected[dao] def insertNodeAction(storageUnit: StorageUnitDto): DBIO[StorageUnitDto] = {
    storageNodeTable returning storageNodeTable.map(_.id) into ((su, id) =>
      su.copy(id = Some(id))) += storageUnit
  }

  /**
   * TODO: Document me!!!
   */
  protected[dao] def updateNodeAction(
    id: StorageNodeId,
    storageUnit: StorageUnitDto
  ): DBIO[Int] = {
    storageNodeTable.filter { su =>
      su.id === id &&
        su.isDeleted === false &&
        su.storageType === storageUnit.storageType
    }.update(storageUnit)
  }

  class StorageNodeTable(
      val tag: Tag
  ) extends Table[StorageUnitDto](tag, SchemaName, "STORAGE_NODE") {
    // scalastyle:off method.name      id.?,

    def * = (
      id.?,
      storageType,
      storageUnitName,
      area,
      areaTo,
      isPartOf,
      height,
      heightTo,
      groupRead,
      groupWrite,
      isDeleted,
      museumId
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[StorageNodeId]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)
    val storageType = column[StorageType]("STORAGE_TYPE")
    val storageUnitName = column[String]("STORAGE_NODE_NAME")
    val area = column[Option[Double]]("AREA")
    val areaTo = column[Option[Double]]("AREA_TO")
    val isPartOf = column[Option[StorageNodeId]]("IS_PART_OF")
    val height = column[Option[Double]]("HEIGHT")
    val heightTo = column[Option[Double]]("HEIGHT_TO")
    val groupRead = column[Option[String]]("GROUP_READ")
    val groupWrite = column[Option[String]]("GROUP_WRITE")
    val isDeleted = column[Boolean]("IS_DELETED")
    val museumId = column[MuseumId]("MUSEUM_ID")

    def create = (
      id: Option[StorageNodeId],
      storageType: StorageType,
      storageNodeName: String,
      area: Option[Double],
      areaTo: Option[Double],
      isPartOf: Option[StorageNodeId],
      height: Option[Double],
      heightTo: Option[Double],
      groupRead: Option[String],
      groupWrite: Option[String],
      isDeleted: Boolean,
      museumId: MuseumId
    ) =>
      StorageUnitDto(
        id = id,
        storageType = storageType,
        name = storageNodeName,
        area = area,
        areaTo = areaTo,
        isPartOf = isPartOf,
        height = height,
        heightTo = heightTo,
        groupRead = groupRead,
        groupWrite = groupWrite,
        isDeleted = Option(isDeleted),
        museumId = museumId
      )

    def destroy(unit: StorageUnitDto) =
      Some((
        unit.id,
        unit.storageType,
        unit.name,
        unit.area,
        unit.areaTo,
        unit.isPartOf,
        unit.height,
        unit.heightTo,
        unit.groupRead,
        unit.groupWrite,
        unit.isDeleted.getOrElse(false),
        unit.museumId
      ))
  }

}