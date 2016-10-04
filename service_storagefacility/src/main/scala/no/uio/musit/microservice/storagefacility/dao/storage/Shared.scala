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
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{BuildingDto, OrganisationDto, RoomDto, StorageUnitDto}
import no.uio.musit.microservice.storagefacility.domain.storage.{StorageNodeId, StorageType}
import no.uio.musit.microservice.storagefacility.domain.{NamedPathElement, NodePath}
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

private[dao] trait BaseStorageDao extends HasDatabaseConfigProvider[JdbcProfile]

private[dao] trait SharedStorageTables extends BaseStorageDao
    with ColumnTypeMappers {

  private val logger = Logger(classOf[SharedStorageTables])

  import driver.api._

  protected def wrongNumUpdatedRows(id: StorageNodeId, numRowsUpdated: Int) =
    s"Wrong amount of rows ($numRowsUpdated) updated for node $id"

  protected val rootNodeType: StorageType = StorageType.RootType

  protected val storageNodeTable = TableQuery[StorageNodeTable]

  protected val organisationTable = TableQuery[OrganisationTable]

  protected val buildingTable = TableQuery[BuildingTable]

  protected val roomTable = TableQuery[RoomTable]

  /**
   * TODO: Document me!!!
   *
   * Only returns non-root nodes
   */
  protected[storage] def getUnitByIdAction(
    id: StorageNodeId
  ): DBIO[Option[StorageUnitDto]] = {
    storageNodeTable.filter { sn =>
      sn.id === id && sn.isDeleted === false && sn.storageType =!= rootNodeType
    }.result.headOption
  }

  protected[storage] def getAllByIdAction(
    id: StorageNodeId
  ): DBIO[Option[StorageUnitDto]] = {
    storageNodeTable.filter { sn =>
      sn.id === id && sn.isDeleted === false
    }.result.headOption
  }

  protected[storage] def getPathByIdAction(
    id: StorageNodeId
  ): DBIO[Option[NodePath]] = {
    storageNodeTable.filter { sn =>
      sn.id === id && sn.isDeleted === false
    }.map(_.path).result.headOption
  }

  protected[storage] def updatePathAction(
    id: StorageNodeId,
    path: NodePath
  ): DBIO[Int] = {
    storageNodeTable.filter { sn =>
      sn.id === id && sn.isDeleted === false
    }.map(_.path).update(path)
  }

  protected[storage] def updatePaths(
    oldParent: NodePath,
    newParent: NodePath
  ): DBIO[Int] = {
    val pathFilter = s"${oldParent.path}%"
    val op = oldParent.path
    val np = newParent.path

    logger.debug(s"Using old path: $op and new path: $np")
    logger.debug(s"performing update with LIKE: $pathFilter")

    sql"""
         UPDATE "MUSARK_STORAGE"."STORAGE_NODE"
         SET "NODE_PATH" = replace("NODE_PATH", ${op}, ${np})
         WHERE "NODE_PATH" LIKE ${pathFilter}
       """.asUpdate.transactionally
  }

  /**
   * Action for fetching the names for each StorageNodeId in the provided
   * NodePath attribute.
   *
   * @param nodePath NodePath to get names for
   * @return A {{{DBIO[Seq[NamedPathElement]]}}}
   */
  protected[storage] def namesForPathAction(
    nodePath: NodePath
  ): DBIO[Seq[NamedPathElement]] = {
    storageNodeTable.filter { sn =>
      sn.id inSetBind nodePath.asIdSeq
    }.map(s => (s.id, s.name)).result.map(_.map(t => NamedPathElement(t._1, t._2)))
  }

  /**
   * TODO: Document me!!!
   */
  protected[storage] def insertNodeAction(
    dto: StorageUnitDto
  ): DBIO[StorageNodeId] = {
    storageNodeTable returning storageNodeTable.map(_.id) += dto
  }

  /**
   * TODO: Document me!!!
   */
  protected[storage] def countChildren(id: StorageNodeId): DBIO[Int] = {
    storageNodeTable.filter { sn =>
      sn.isPartOf === id && sn.isDeleted === false
    }.length.result
  }

  /**
   * TODO: Document me!!!
   */
  protected[storage] def updateNodeAction(
    id: StorageNodeId,
    storageUnit: StorageUnitDto
  ): DBIO[Int] = {
    storageNodeTable.filter { sn =>
      sn.id === id &&
        sn.isDeleted === false &&
        sn.storageType === storageUnit.storageType
    }.update(storageUnit)
  }

  /**
   * TODO: Document me!!!
   */
  private[storage] class StorageNodeTable(
      val tag: Tag
  ) extends Table[StorageUnitDto](tag, SchemaName, "STORAGE_NODE") {
    // scalastyle:off method.name
    def * = (
      id.?,
      storageType,
      name,
      area,
      areaTo,
      isPartOf,
      height,
      heightTo,
      groupRead,
      groupWrite,
      isDeleted,
      path
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[StorageNodeId]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)
    val storageType = column[StorageType]("STORAGE_TYPE")
    val name = column[String]("STORAGE_NODE_NAME")
    val area = column[Option[Double]]("AREA")
    val areaTo = column[Option[Double]]("AREA_TO")
    val isPartOf = column[Option[StorageNodeId]]("IS_PART_OF")
    val height = column[Option[Double]]("HEIGHT")
    val heightTo = column[Option[Double]]("HEIGHT_TO")
    val groupRead = column[Option[String]]("GROUP_READ")
    val groupWrite = column[Option[String]]("GROUP_WRITE")
    val isDeleted = column[Boolean]("IS_DELETED")
    val path = column[NodePath]("NODE_PATH")

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
      nodePath: NodePath
    ) =>
      StorageUnitDto(
        id = id,
        name = storageNodeName,
        area = area,
        areaTo = areaTo,
        isPartOf = isPartOf,
        height = height,
        heightTo = heightTo,
        groupRead = groupRead,
        groupWrite = groupWrite,
        path = nodePath,
        isDeleted = Option(isDeleted),
        storageType = storageType
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
        unit.path
      ))
  }

  private[storage] class RoomTable(
      val tag: Tag
  ) extends Table[RoomDto](tag, SchemaName, "ROOM") {
    // scalastyle:off method.name
    def * = (
      id,
      perimeterSecurity,
      theftProtection,
      fireProtection,
      waterDamage,
      routinesAndContingency,
      relativeHumidity,
      temperatureAssessment,
      lighting,
      preventiveConservation
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val id = column[Option[StorageNodeId]]("STORAGE_NODE_ID", O.PrimaryKey)
    val perimeterSecurity = column[Option[Boolean]]("PERIMETER_SECURITY")
    val theftProtection = column[Option[Boolean]]("THEFT_PROTECTION")
    val fireProtection = column[Option[Boolean]]("FIRE_PROTECTION")
    val waterDamage = column[Option[Boolean]]("WATER_DAMAGE_ASSESSMENT")
    val routinesAndContingency = column[Option[Boolean]]("ROUTINES_AND_CONTINGENCY_PLAN")
    val relativeHumidity = column[Option[Boolean]]("RELATIVE_HUMIDITY")
    val temperatureAssessment = column[Option[Boolean]]("TEMPERATURE_ASSESSMENT")
    val lighting = column[Option[Boolean]]("LIGHTING_CONDITION")
    val preventiveConservation = column[Option[Boolean]]("PREVENTIVE_CONSERVATION")

    def create = (
      id: Option[StorageNodeId],
      perimeterSecurity: Option[Boolean],
      theftProtection: Option[Boolean],
      fireProtection: Option[Boolean],
      waterDamage: Option[Boolean],
      routinesAndContingency: Option[Boolean],
      relativeHumidity: Option[Boolean],
      temperature: Option[Boolean],
      lighting: Option[Boolean],
      preventiveConservation: Option[Boolean]
    ) =>
      RoomDto(
        id = id,
        perimeterSecurity = perimeterSecurity,
        theftProtection = theftProtection,
        fireProtection = fireProtection,
        waterDamageAssessment = waterDamage,
        routinesAndContingencyPlan = routinesAndContingency,
        relativeHumidity = relativeHumidity,
        temperatureAssessment = temperature,
        lightingCondition = lighting,
        preventiveConservation = preventiveConservation
      )

    def destroy(room: RoomDto) =
      Some((
        room.id,
        room.perimeterSecurity,
        room.theftProtection,
        room.fireProtection,
        room.waterDamageAssessment,
        room.routinesAndContingencyPlan,
        room.relativeHumidity,
        room.temperatureAssessment,
        room.lightingCondition,
        room.preventiveConservation
      ))
  }

  private[storage] class BuildingTable(
      val tag: Tag
  ) extends Table[BuildingDto](tag, SchemaName, "BUILDING") {

    def * = (id.?, address) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[StorageNodeId]("STORAGE_NODE_ID", O.PrimaryKey)
    val address = column[Option[String]]("POSTAL_ADDRESS")

    def create = (id: Option[StorageNodeId], address: Option[String]) =>
      BuildingDto(
        id = id,
        address = address
      )

    def destroy(building: BuildingDto) =
      Some((building.id, building.address))
  }

  private[storage] class OrganisationTable(
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