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
import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageNodeDto
import no.uio.musit.microservice.storagefacility.domain.{NamedPathElement, NodePath}
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
   * Check to see if the node with the provided StorageNodeId exists or not.
   *
   * @param id
   * @return
   */
  def exists(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Boolean]] = {
    val query = storageNodeTable.filter { su =>
      su.museumId === mid && su.id === id && su.isDeleted === false
    }.exists.result

    db.run(query).map(found => MusitSuccess(found)).recover {
      case NonFatal(ex) =>
        logger.error("Non fatal exception when checking for node existence", ex)
        MusitDbError("Checking if node exists caused an exception", Option(ex))
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getById(mid: MuseumId, id: StorageNodeId): Future[Option[StorageUnit]] = {
    val query = getUnitByIdAction(mid, id)
    db.run(query).map(_.map(StorageNodeDto.toStorageUnit))
  }

  /**
   * TODO: Document me!!!
   */
  def getNodeById(mid: MuseumId, id: StorageNodeId): Future[Option[GenericStorageNode]] = {
    val query = getNodeByIdAction(mid, id)
    db.run(query).map(_.map(StorageNodeDto.toGenericStorageNode))
  }

  /**
   * TODO: Document me!!!
   */
  def getStorageTypesInPath(
    mid: MuseumId,
    path: NodePath,
    limit: Int
  ): Future[Seq[(StorageNodeId, StorageType)]] = {
    val ids = path.asIdSeq.take(limit)
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
      sn.id.inSet(ids)
    }.map(res => (res.id, res.storageType)).result
    db.run(query)
  }

  /**
   * Find all nodes that are of type Root.
   *
   * @return a Future collection of Root nodes.
   */
  def findRootNodes(mid: MuseumId): Future[Seq[Root]] = {
    val query = storageNodeTable.filter { root =>
      root.museumId === mid &&
        root.isDeleted === false &&
        root.isPartOf.isEmpty &&
        root.storageType === rootNodeType
    }.result

    db.run(query).map(_.map(n => Root(n.id)))
  }

  /**
   * find the Root node with the given StorageNodeId.
   *
   * @param id StorageNodeId for the Root node.
   * @return An Option that contains the Root node if it was found.
   */
  def findRootNode(id: StorageNodeId): Future[MusitResult[Option[Root]]] = {
    val query = storageNodeTable.filter { root =>
      root.id === id &&
        root.isDeleted === false &&
        root.storageType === rootNodeType
    }.result.headOption

    db.run(query).map { dto =>
      MusitSuccess(dto.map(n => Root(id = n.id, path = n.path)))
    }
  }

  /**
   * TODO: Document me!!!
   */
  def getChildren(mid: MuseumId, id: StorageNodeId): Future[Seq[GenericStorageNode]] = {
    val query = storageNodeTable.filter { node =>
      node.museumId === mid && node.isPartOf === id && node.isDeleted === false
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
  def insert(mid: MuseumId, storageUnit: StorageUnit): Future[StorageNodeId] = {
    val dto = StorageNodeDto.fromStorageUnit(mid, storageUnit)
    db.run(insertNodeAction(dto))
  }

  /**
   * TODO: Document me!!!
   */
  def insertRoot(mid: MuseumId, root: Root): Future[StorageNodeId] = {
    logger.debug("Inserting root node...")
    val dto = StorageNodeDto.fromRoot(mid, root).asStorageUnit(mid)
    db.run(insertNodeAction(dto))
  }

  /**
   * Set the path for the Root with the given StorageNodeId.
   *
   * @param id   StorageNodeId of the Root node.
   * @param path NodePath to set
   * @return An Option containing the updated Root node.
   */
  def setRootPath(id: StorageNodeId, path: NodePath): Future[MusitResult[Unit]] = {
    logger.debug(s"Updating path to $path for root node $id")
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 =>
        MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Updates the path for all nodes that starts with the "oldPath".
   *
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
   * Updates all paths for the subtree of the given StorageNodeId
   *
   * @param id      StorageNodeId
   * @param oldPath NodePath representing the old path
   * @param newPath NodePath representing the new path
   * @return The number of paths updated.
   */
  def updatePathForSubTree(
    id: StorageNodeId,
    oldPath: NodePath,
    newPath: NodePath
  ): Future[MusitResult[Int]] = {
    db.run(updatePaths(oldPath, newPath)).map {
      case res: Int if res != 0 =>
        logger.debug(s"Successfully updated path for $res nodes")
        MusitSuccess(res)
      case _ =>
        val msg = s"Did not update any paths starting with $oldPath"
        logger.error(msg)
        MusitInternalError(msg)

    }.recover {
      case NonFatal(ex) =>
        val msg = s"Unexpected error when updating paths for unit $id sub-tree"
        logger.error(msg, ex)
        MusitDbError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def update(
    mid: MuseumId,
    id: StorageNodeId,
    storageUnit: StorageUnit
  ): Future[MusitResult[Option[Int]]] = {
    val dto = StorageNodeDto.fromStorageUnit(mid, storageUnit)
    db.run(updateNodeAction(mid, id, dto)).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
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
  def markAsDeleted(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Int]] = {
    val query = storageNodeTable.filter { su =>
      su.id === id && su.isDeleted === false && su.museumId === mid
    }.map(_.isDeleted).update(true)

    db.run(query).map {
      case res: Int if res == 1 =>
        MusitSuccess(res)

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
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

    db.run(query).map {
      case res: Int if res == 1 =>
        MusitSuccess(res)

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Given the provided NodePath, fetch all the associated names for each of
   * the ID's in the path.
   *
   * @param nodePath NodePath to find names for
   * @return A Seq[NamedPathElement]
   */
  def namesForPath(nodePath: NodePath): Future[Seq[NamedPathElement]] = {
    db.run(namesForPathAction(nodePath))
  }

}
