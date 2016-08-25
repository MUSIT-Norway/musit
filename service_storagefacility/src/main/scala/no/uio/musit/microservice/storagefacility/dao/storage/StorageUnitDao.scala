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
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDTO
import no.uio.musit.microservice.storagefacility.domain.storage.{ Storage, StorageType }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  implicit lazy val storageTypeMapper = MappedColumnType.base[StorageType, String](
    storageType => storageType.entryName,
    string => StorageType.withName(string)
  )

  private val StorageUnitTable = TableQuery[StorageUnitTable]

  def unknownStorageUnitMsg(id: Long) = s"Unknown storageUnit with id: $id"

  def storageUnitNotFoundError(id: Long): MusitError =
    ErrorHelper.notFound(unknownStorageUnitMsg(id))

  def getStorageUnitOnlyById(id: Long): Future[Option[StorageUnitDTO]] =
    db.run(StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).result.headOption)

  def getChildren(id: Long): Future[Seq[StorageUnitDTO]] = {
    val action = StorageUnitTable.filter(_.isPartOf === id).result
    db.run(action)
  }

  def getStorageType(id: Long): MusitFuture[StorageType] = {
    db.run(StorageUnitTable.filter(_.id === id).map(_.storageType).result.headOption)
      .foldInnerOption(Left(storageUnitNotFoundError(id)), Right(_))
  }

  def all(): Future[Seq[StorageUnitDTO]] =
    db.run(StorageUnitTable.filter(st => st.isDeleted === false).result)

  def insert(storageUnit: StorageUnitDTO): Future[StorageUnitDTO] =
    db.run(insertAction(storageUnit))

  def insertAction(storageUnit: StorageUnitDTO): DBIO[StorageUnitDTO] = {
    StorageUnitTable returning StorageUnitTable.map(_.id) into
      ((storageUnit, id) =>
        storageUnit.copy(id = Some(id), links = Storage.linkText(Some(id)))) +=
      storageUnit
  }

  def updateStorageUnitAction(id: Long, storageUnit: StorageUnitDTO): DBIO[Int] = {
    StorageUnitTable.filter(st => st.id === id && st.isDeleted === false).update(storageUnit)
  }

  def updateStorageUnit(id: Long, storageUnit: StorageUnitDTO): Future[Int] = {
    db.run(updateStorageUnitAction(id, storageUnit))
  }

  def deleteStorageUnit(id: Long): Future[Int] = {
    db.run((for {
      storageUnit <- StorageUnitTable if storageUnit.id === id && storageUnit.isDeleted === false
    } yield storageUnit.isDeleted).update(true))
  }

  private class StorageUnitTable(tag: Tag) extends Table[StorageUnitDTO](tag, Some("MUSARK_STORAGE"), "STORAGE_UNIT") {
    def * = (id.?, storageType, storageUnitName, area, areaTo, isPartOf, height, heightTo, groupRead, groupWrite, isDeleted) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("STORAGE_UNIT_ID", O.PrimaryKey, O.AutoInc)

    val storageType = column[StorageType]("STORAGE_TYPE")

    val storageUnitName = column[String]("STORAGE_UNIT_NAME")

    val area = column[Option[Long]]("AREA")

    val areaTo = column[Option[Long]]("AREA_TO")

    val isPartOf = column[Option[Long]]("IS_PART_OF")

    val height = column[Option[Long]]("HEIGHT")

    val heightTo = column[Option[Long]]("HEIGHT_TO")

    val groupRead = column[Option[String]]("GROUP_READ")

    val groupWrite = column[Option[String]]("GROUP_WRITE")

    val isDeleted = column[Boolean]("IS_DELETED")

    def create = (
      id: Option[Long],
      storageType: StorageType,
      storageUnitName: String,
      area: Option[Long],
      areaTo: Option[Long],
      isPartOf: Option[Long],
      height: Option[Long],
      heightTo: Option[Long],
      groupRead: Option[String],
      groupWrite: Option[String],
      isDeleted: Boolean
    ) =>
      StorageUnitDTO(
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

    def destroy(unit: StorageUnitDTO) =
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
