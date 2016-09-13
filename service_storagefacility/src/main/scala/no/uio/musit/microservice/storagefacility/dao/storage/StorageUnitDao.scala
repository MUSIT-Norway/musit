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
import no.uio.musit.microservice.storagefacility.domain.MusitResults._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.{ StorageNodeDto, StorageUnitDto }
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */

// TODO: Change public API methods to use MusitResult[A]
@Singleton
class StorageUnitDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  val logger = Logger(classOf[StorageUnitDao])

  private val storageUnitTable = TableQuery[StorageUnitTable]

  protected[dao] def getByIdAction(id: StorageNodeId): DBIO[Option[StorageUnitDto]] = {
    storageUnitTable.filter { st =>
      st.id === id && st.isDeleted === false
    }.result.headOption
  }

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[StorageUnit]] = {
    val query = getByIdAction(id)
    db.run(query).map { dto =>
      dto.map(unitDto => StorageNodeDto.toStorageUnit(unitDto))
    }
  }

  // FIXME: I do not like this method.
  // It leaves the type checking and DTO conversion to the service layer.
  def getNodeById(id: StorageNodeId): Future[Option[StorageUnitDto]] = {
    val query = getByIdAction(id)
    db.run(query)
  }

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
  def getStorageType(id: StorageNodeId): Future[MusitResult[Option[StorageType]]] = {
    db.run(
      storageUnitTable.filter(_.id === id).map(_.storageType).result.headOption
    ).map { maybeStorageType =>
      MusitSuccess(maybeStorageType)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(storageUnit: StorageUnit): Future[StorageUnit] = {
    val dto = StorageNodeDto.fromStorageUnit(storageUnit)
    db.run(insertAction(dto)).map(StorageNodeDto.toStorageUnit)
  }

  protected[dao] def insertAction(storageUnit: StorageUnitDto): DBIO[StorageUnitDto] = {
    storageUnitTable returning storageUnitTable.map(_.id) into (
      (su, id) =>
        su.copy(
          id = Some(id)
        //          , links = Storage.linkText(Some(id))
        )
    ) += storageUnit
  }

  /**
   * TODO: Document me!!!
   */
  protected[dao] def updateAction(
    id: StorageNodeId,
    storageUnit: StorageUnitDto
  ): DBIO[Int] = {
    storageUnitTable.filter { su =>
      su.id === id && su.isDeleted === false
    }.update(storageUnit)
  }

  /**
   * TODO: Document me!!!
   */
  def update(
    id: StorageNodeId,
    storageUnit: StorageUnitDto
  ): Future[Option[StorageUnit]] = {
    db.run(updateAction(id, storageUnit)).flatMap {
      case res: Int if res > 1 || res < 0 =>
        logger.warn("Wrong amount of rows updated")
        Future.successful(None)

      case res: Int =>
        getById(id)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def markAsDeleted(id: StorageNodeId): Future[MusitResult[Int]] = {
    db.run(
      (for {
        su <- storageUnitTable if su.id === id && su.isDeleted === false
      } yield su.isDeleted).update(true)
    ).map { res =>
        if (res == 0) MusitSuccess(res)
        else MusitValidationError(
          message = "Unexpected result marking storagenode as deleted",
          expected = 0,
          actual = res
        )
      }
  }

}
