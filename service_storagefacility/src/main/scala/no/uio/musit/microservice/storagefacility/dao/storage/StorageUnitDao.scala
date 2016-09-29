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
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageNodeDto
import no.uio.musit.service.MusitResults._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

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

  /**
   * TODO: Document me!!!
   */
  def getById(id: StorageNodeId): Future[Option[StorageUnit]] = {
    val query = getUnitByIdAction(id)
    db.run(query).map(_.map(StorageNodeDto.toStorageUnit))
  }

  /**
   * Find all nodes that are of type Root.
   *
   * @return a Future collection of Root nodes.
   */
  def findRootNodes: Future[Seq[Root]] = {
    val query = storageNodeTable.filter { root =>
      root.isDeleted === false &&
        root.isPartOf.isEmpty &&
        root.storageType === rootNodeType
    }.result

    db.run(query).map(_.map(n => Root(n.id)))
  }

  def findRootNode(id: StorageNodeId): Future[Option[Root]] = {
    val query = storageNodeTable.filter { root =>
      root.id === id &&
        root.isDeleted === false &&
        root.storageType === rootNodeType
    }.result.headOption

    db.run(query).map(_.map(n => Root(id = n.id, path = Option(n.path))))
  }

  /**
   * TODO: Document me!!!
   */
  def getChildren(id: StorageNodeId): Future[Seq[GenericStorageNode]] = {
    val query = storageNodeTable.filter { node =>
      node.isPartOf === id && node.isDeleted === false
    }.result
    db.run(query).map { dtos =>
      dtos.map { dto =>
        StorageNodeDto.toGenericStorageNode(dto)
      }
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getStorageType(id: StorageNodeId): Future[MusitResult[Option[StorageType]]] = {
    db.run(
      storageNodeTable.filter { node =>
      node.id === id && node.isDeleted === false
    }.map(_.storageType).result.headOption
    ).map { maybeStorageType =>
      MusitSuccess(maybeStorageType)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(storageUnit: StorageUnit): Future[StorageUnit] = {
    val dto = StorageNodeDto.fromStorageUnit(storageUnit)
    db.run(insertNodeAction(dto)).map(StorageNodeDto.toStorageUnit)
  }

  /**
   * TODO: Document me!!!
   */
  def insertRoot(root: Root): Future[Root] = {
    logger.debug("Inserting root node...")
    val dto = StorageNodeDto.fromRoot(root).asStorageUnit
    db.run(insertNodeAction(dto)).map { sudto =>
      logger.debug(s"Inserted root node with ID ${sudto.id}")
      Root(sudto.id)
    }
  }

  def updateRootPath(id: StorageNodeId, path: NodePath): Future[Option[Root]] = {
    logger.debug(s"Updating path to $path for root node $id")
    db.run(updatePathAction(id, path)).flatMap {
      case res: Int if res == 1 =>
        findRootNode(id)

      case res: Int =>
        logger.warn(s"Wrong amount of rows ($res) updated")
        Future.successful(None)
    }
  }

  /**
   * Applies one update for each of tupled StorageNodeId and NodePath varargs
   *
   * @param tuple A vararg tuple of StorageNodeId and NodePath
   * @return a Seq of int indicating how many entries were updated per tuple.
   */
  def updatePaths(tuple: (StorageNodeId, NodePath)*): Future[Seq[Int]] = {
    val actions = DBIO.sequence(tuple.map(t => updatePathAction(t._1, t._2)))
    db.run(actions).map { res =>
      logger.debug(s"Updated $res paths")
      res
    }
  }

  /**
   * Updates the path for the given StoragNodeId
   * @param id the StorageNodeId to update
   * @param path the NodePath to set
   * @return the updated StorageUnit
   */
  def updatePath(id: StorageNodeId, path: NodePath): Future[Option[StorageUnit]] = {
    db.run(updatePathAction(id, path)).flatMap {
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
  def update(
    id: StorageNodeId,
    storageUnit: StorageUnit
  ): Future[Option[StorageUnit]] = {
    val dto = StorageNodeDto.fromStorageUnit(storageUnit)
    db.run(updateNodeAction(id, dto)).flatMap {
      case res: Int if res == 1 =>
        getById(id)

      case res: Int =>
        logger.warn(s"Wrong amount of rows ($res) updated")
        Future.successful(None)
    }
  }

  /**
   * Find and return the NodePath for the given StorageNodeId.
   *
   * @param id StorageNodeId to get the NodePath for
   * @return NodePath
   */
  def getPathById(id: StorageNodeId): Future[Option[NodePath]] = {
    db.run(getPathByIdAction(id))
  }

  /**
   * TODO: Document me!!!
   */
  def markAsDeleted(id: StorageNodeId): Future[MusitResult[Int]] = {
    val query = storageNodeTable.filter { su =>
      su.id === id && su.isDeleted === false
    }.map(_.isDeleted).update(true)

    db.run(query).map { res =>
      if (res == 1) MusitSuccess(res)
      else MusitValidationError(
        message = s"Unexpected result marking storage node $id as deleted",
        expected = 1,
        actual = res
      )
    }
  }

  /**
   * TODO: Document me!!!
   */
  def updatePartOf(
    id: StorageNodeId,
    partOf: Option[StorageNodeId]
  ): Future[MusitResult[Int]] = {
    val filter = storageNodeTable.filter(n =>
      n.id === id && n.isDeleted === false)
    val q = for { n <- filter } yield n.isPartOf
    val query = q.update(partOf)

    db.run(query).map { res =>
      if (res == 1) MusitSuccess(res)
      else MusitValidationError(
        message = s"Unexpected result updating partOf for storage node $id",
        expected = 1,
        actual = res
      )
    }
  }

}
