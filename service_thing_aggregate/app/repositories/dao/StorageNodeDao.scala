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

import com.google.inject.Inject
import no.uio.musit.models._
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

class StorageNodeDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val storageNodeTable = TableQuery[StorageNodeTable]
  private val localObjectTable = TableQuery[LocalObjectsTable]

  def nodeExists(
    mid: MuseumId,
    nodeId: StorageNodeId
  ): Future[MusitResult[Boolean]] = {
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
        sn.id === nodeId
    }.length.result
    db.run(query).map(res => MusitSuccess(res == 1)).recover {
      case NonFatal(e) =>
        MusitDbError(s"Error occurred while checking for node existence for " +
          s"nodeId $nodeId", Some(e))
    }
  }

  def currentLocation(
    mid: MuseumId,
    objectId: ObjectId
  ): Future[Option[(StorageNodeId, NodePath)]] = {
    val findLocalObjectAction = localObjectTable.filter { lo =>
      lo.museumId === mid && lo.objectId === objectId
    }.map(_.currentLocationId).result.headOption

    val findPathAction = (maybeId: Option[StorageNodeId]) => maybeId.map { nodeId =>
      storageNodeTable.filter(_.id === nodeId).map(_.path).result.headOption
    }.getOrElse(DBIO.successful(None))

    val query = for {
      maybeNodeId <- findLocalObjectAction
      maybePath <- findPathAction(maybeNodeId)
    } yield maybeNodeId.flatMap(nid => maybePath.map(p => (nid, p)))

    db.run(query)
  }

  def namesForPath(nodePath: NodePath): Future[Seq[NamedPathElement]] = {
    val query = storageNodeTable.filter { sn =>
      sn.id inSetBind nodePath.asIdSeq
    }.map(s => (s.id, s.name)).result.map(_.map(t => NamedPathElement(t._1, t._2)))
    db.run(query)
  }

  type TableType = (Option[StorageNodeId], String, String, Option[Double], Option[Double], Option[Long], Option[Double], Option[Double], Option[String], Option[String], Boolean, MuseumId, NodePath) // scalastyle:ignore

  private class StorageNodeTable(
      val tag: Tag
  ) extends Table[TableType](tag, Some("MUSARK_STORAGE"), "STORAGE_NODE") {
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
      museumId,
      path
    )

    // scalastyle:on method.name

    val id = column[StorageNodeId]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)
    val storageType = column[String]("STORAGE_TYPE")
    val name = column[String]("STORAGE_NODE_NAME")
    val area = column[Option[Double]]("AREA")
    val areaTo = column[Option[Double]]("AREA_TO")
    val isPartOf = column[Option[Long]]("IS_PART_OF")
    val height = column[Option[Double]]("HEIGHT")
    val heightTo = column[Option[Double]]("HEIGHT_TO")
    val groupRead = column[Option[String]]("GROUP_READ")
    val groupWrite = column[Option[String]]("GROUP_WRITE")
    val isDeleted = column[Boolean]("IS_DELETED")
    val museumId = column[MuseumId]("MUSEUM_ID")
    val path = column[NodePath]("NODE_PATH")
  }

  type LocObjTable = (ObjectId, EventId, StorageNodeId, MuseumId)

  private class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocObjTable](tag, Some("MUSARK_STORAGE"), "LOCAL_OBJECT") {

    def * = (objectId, latestMoveId, currentLocationId, museumId) // scalastyle:ignore

    val objectId = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeId]("CURRENT_LOCATION_ID")
    val museumId = column[MuseumId]("MUSEUM_ID")
  }

}
