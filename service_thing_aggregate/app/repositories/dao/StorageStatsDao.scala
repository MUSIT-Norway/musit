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

package repositories.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{NodePath, StorageNodeDatabaseId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class StorageStatsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends Tables {

  val logger = Logger(classOf[StorageStatsDao])

  import driver.api._

  private def countChildren(id: StorageNodeDatabaseId): DBIO[Int] = {
    nodeTable.filter { sn =>
      sn.isPartOf === id && sn.isDeleted === false
    }.length.result
  }

  /**
   * Count of *all* children of this node, irrespective of access rights to
   * the children
   */
  def numChildren(id: StorageNodeDatabaseId): Future[MusitResult[Int]] = {
    db.run(countChildren(id)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred counting number node children under $id"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * The total number of museum objects at node or any of its child nodes.
   *
   * @param path NodePath to count total object count for.
   * @return Future[Int] with total number of objects under the provided node
   *         and all its child nodes.
   */
  def numObjectsInPath(path: NodePath): Future[MusitResult[Int]] = {
    val nodeFilter = s"${path.path}%"

    val q1 = nodeTable.filter(_.path.asColumnOf[String] like nodeFilter)

    val q2 = for {
      (sn, lo) <- q1 join locObjTable on (_.id === _.currentLocationId)
    } yield sn

    db.run(q2.length.result).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred counting total objects for nodes in path $path"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  /**
   * The number of museum objects directly at the given node.
   * To calculate the total number of objects for nodes in the tree,
   * use the {{{totalObjectCount}}} method.
   *
   * @param nodeId StorageNodeId to count objects for.
   * @return Future[Int] with the number of objects directly on the provided nodeId
   */
  def numObjectsInNode(nodeId: StorageNodeDatabaseId): Future[MusitResult[Int]] = {
    db.run(
      locObjTable.filter(_.currentLocationId === nodeId).length.result
    ).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = s"An error occurred counting number direct objects in $nodeId"
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}