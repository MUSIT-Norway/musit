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
import no.uio.musit.microservice.storagefacility.dao.ColumnTypeMappers
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import no.uio.musit.microservice.storagefacility.domain.storage.{ Storage, StorageNodeId, StorageType }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val storageUnitTable = TableQuery[StorageUnitTable]

  def unknownStorageUnitMsg(id: StorageNodeId) =
    s"Unknown storageUnit with id: ${id.underlying}"

  def storageUnitNotFoundError(id: StorageNodeId): MusitError =
    ErrorHelper.notFound(unknownStorageUnitMsg(id))

  /**
   * TODO: Document me!!!
   */
  def getStorageUnitOnlyById(id: StorageNodeId): Future[Option[StorageUnitDto]] =
    db.run(storageUnitTable.filter(st => st.id === id && st.isDeleted === false).result.headOption)

  /**
   * TODO: Document me!!!
   */
  def getChildren(id: StorageNodeId): Future[Seq[StorageUnitDto]] = {
    val action = storageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  /**
   * TODO: Document me!!!
   */
  def getStorageType(id: StorageNodeId): MusitFuture[StorageType] = {
    db.run(storageUnitTable.filter(_.id === id).map(_.storageType).result.headOption)
      .foldInnerOption(Left(storageUnitNotFoundError(id)), Right(_))
  }

  /**
   * TODO: Document me!!!
   */
  def all(): Future[Seq[StorageUnitDto]] =
    db.run(storageUnitTable.filter(st => st.isDeleted === false).result)

  /**
   * TODO: Document me!!!
   */
  def insert(storageUnit: StorageUnitDto): Future[StorageUnitDto] =
    db.run(insertAction(storageUnit))

  /**
   * TODO: Document me!!!
   */
  def insertAction(storageUnit: StorageUnitDto): DBIO[StorageUnitDto] = {
    storageUnitTable returning storageUnitTable.map(_.id) into
      ((storageUnit, id) =>
        storageUnit.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageUnit
  }

  /**
   * TODO: Document me!!!
   */
  def updateStorageUnitAction(id: StorageNodeId, storageUnit: StorageUnitDto): DBIO[Int] = {
    storageUnitTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  /**
   * TODO: Document me!!!
   */
  def updateStorageUnit(id: StorageNodeId, storageUnit: StorageUnitDto): Future[Int] = {
    db.run(updateStorageUnitAction(id, storageUnit))
  }

  /**
   * TODO: Document me!!!
   */
  def deleteStorageUnit(id: StorageNodeId): Future[Int] = {
    db.run((for {
      storageUnit <- storageUnitTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnitDto](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id.?, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[StorageNodeId]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[StorageType]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_UNIT_NAME")

    val area = column[Option[Long]]("AREA")

    val areaTo = column[Option[Long]]("AREA_TO")

    val isPartOf = column[Option[StorageNodeId]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val heightTo = column[Option[Long]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val isDeleted = column[Boolean]("IS_DELETED")

    def create = (
      id: Option[StorageNodeId],
      storageType: StorageType,
      storageUnitName: String,
      area: Option[Long],
      areaTo: Option[Long],
      isPartOf: Option[StorageNodeId],
      height: Option[Long],
      heightTo: Option[Long],
      groupRead: Option[String],
      groupWrite: Option[String],
      isDeleted: Boolean
    ) =>
      StorageUnitDto(
        id,
        storageUnitName,
        area,
        areaTo,
        isPartOf,
        height,
        heightTo,
        groupRead,
        groupWrite,
        Storage.linkText(id),
        Option(isDeleted),
        storageType
      )

    def destroy(unit: StorageUnitDto) =
      Some(
        unit.id,
        unit.`type`,
        unit.name,
        unit.area,
        unit.areaTo,
        unit.isPartOf,
        unit.height,
        unit.heightTo,
        unit.groupRead,
        unit.groupWrite,
        unit.isDeleted.getOrElse(false)
      )
  }

}
