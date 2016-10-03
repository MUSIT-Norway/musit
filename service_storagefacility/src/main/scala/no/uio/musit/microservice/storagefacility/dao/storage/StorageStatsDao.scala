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
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage.{StorageNode, StorageNodeId}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

@Singleton
class StorageStatsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedStorageTables {

  import driver.api._

  /**
   * Count of *all* children of this node, irrespective of access rights to
   * the children
   */
  def childCount(id: StorageNodeId): Future[Int] = {
    db.run(countChildren(id))
  }

  /**
   * The total number of museum objects at node or any of its child nodes.
   *
   * @param path NodePath to count total object count for.
   * @return Future[Int] with total number of objects under the provided node
   *         and all its child nodes.
   */
  def totalObjectCount(path: NodePath): Future[Int] = {
    val nodeFilter = s"${path.path}%"
    db.run(
      sql"""
        select count(*)
        from musark_storage.storage_node n, musark_storage.local_object o
        where n.node_path like ${nodeFilter}
        and o.current_location_id = n.storage_node_id
      """.as[Int].head
    )
  }

  /**
   * The number of museum objects directly at the given node.
   * To calculate the total number of objects for nodes in the tree,
   * use the {{{totalObjectCount}}} method.
   *
   * @param nodeId StorageNodeId to count objects for.
   * @return Future[Int] with the number of objects directly on the provided nodeId
   */
  def directObjectCount(nodeId: StorageNodeId): Future[Int] = {
    db.run(
      sql"""
        select count(*)
        from musark_storage.local_object o
        where o.current_location_id = ${nodeId.underlying}
      """.as[Int].head
    )
  }

}