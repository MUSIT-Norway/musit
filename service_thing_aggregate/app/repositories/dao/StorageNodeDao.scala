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
import controllers.SimpleNode
import no.uio.musit.models._
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

class StorageNodeDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  private val logger = Logger(classOf[StorageNodeDao])

  import driver.api._

  private val storageNodeTable = TableQuery[StorageNodeTable]
  private val localObjectTable = TableQuery[LocalObjectsTable]

  def nodeExists(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Boolean]] = {
    val query = storageNodeTable.filter { sn =>
      sn.museumId === mid &&
        sn.id === nodeId
    }.length.result
    db.run(query).map(res => MusitSuccess(res == 1)).recover {
      case NonFatal(e) =>
        val msg = s"Error occurred while checking for node existence for nodeId $nodeId"
        logger.error(msg, e)
        MusitDbError(msg, Option(e))
    }
  }

  def currentLocation(
    mid: MuseumId,
    objectId: ObjectId
  ): Future[Option[(StorageNodeDatabaseId, NodePath)]] = {
    val findLocalObjectAction = localObjectTable.filter { lo =>
      lo.museumId === mid && lo.objectId === objectId
    }.map(_.currentLocationId).result.headOption

    val findPathAction = (maybeId: Option[StorageNodeDatabaseId]) =>
      maybeId.map { nodeId =>
        storageNodeTable.filter(_.id === nodeId).map(_.path).result.headOption
      }.getOrElse(DBIO.successful(None))

    val query = for {
      maybeNodeId <- findLocalObjectAction
      maybePath <- findPathAction(maybeNodeId)
    } yield maybeNodeId.flatMap(nid => maybePath.map(p => (nid, p)))

    db.run(query).recover {
      case NonFatal(e) =>
        val msg = s"Error occurred while getting current location for object $objectId"
        logger.error(msg, e)
        None
    }
  }

  def namesForPath(nodePath: NodePath): Future[Seq[NamedPathElement]] = {
    val query = storageNodeTable.filter { sn =>
      sn.id inSetBind nodePath.asIdSeq
    }.map(s => (s.id, s.name)).result.map(_.map(t => NamedPathElement(t._1, t._2)))
    db.run(query).recover {
      case NonFatal(e) =>
        val msg = s"Error occurred while fetching named path for $nodePath"
        logger.error(msg, e)
        Seq.empty
    }
  }

  def getRootLoanNodes(
    museumId: MuseumId
  ): Future[MusitResult[Seq[StorageNodeDatabaseId]]] = {
    val query = storageNodeTable.filter { n =>
      n.museumId === museumId && n.storageType === "RootLoan"
    }.map(_.id)

    db.run(query.result).map(nodes => MusitSuccess(nodes)).recover {
      case NonFatal(e) =>
        val msg = s"Error occurred getting RootLoan nodes for museum $museumId"
        logger.error(msg, e)
        MusitDbError(msg, Option(e))
    }
  }

  def listAllChildrenFor(
    museumId: MuseumId,
    ids: Seq[StorageNodeDatabaseId]
  ): Future[MusitResult[Seq[SimpleNode]]] = {
    val q1 = (likePath: String) => storageNodeTable.filter { n =>
      n.museumId === museumId && (n.path.asColumnOf[String] like likePath)
    }
    val query = ids.map(id => s",${id.underlying},%")
      .map(q1)
      .reduce((query, queryPart) => query union queryPart)
      .map(n => (n.id, n.name))

    db.run(query.result).map { res =>
      MusitSuccess(
        res.map(r => (r._1, r._2))
      )
    }.recover {
      case NonFatal(e) =>
        val msg = s"Error occurred reading children for RootLoan " +
          s"nodes ${ids.mkString(", ")}"
        logger.error(msg, e)
        MusitDbError(msg, Option(e))
    }
  }

  type TableType = (Option[StorageNodeDatabaseId], String, String, Option[Double], Option[Double], Option[Long], Option[Double], Option[Double], Option[String], Option[String], Boolean, MuseumId, NodePath) // scalastyle:ignore

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

    val id = column[StorageNodeDatabaseId]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)
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

  type LocObjTable = (ObjectId, EventId, StorageNodeDatabaseId, MuseumId)

  private class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocObjTable](tag, Some("MUSARK_STORAGE"), "LOCAL_OBJECT") {

    def * = (objectId, latestMoveId, currentLocationId, museumId) // scalastyle:ignore

    val objectId = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeDatabaseId]("CURRENT_LOCATION_ID")
    val museumId = column[MuseumId]("MUSEUM_ID")
  }

}
