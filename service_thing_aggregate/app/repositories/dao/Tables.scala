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

import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

trait Tables extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  // Type aliases representing rows for the different tables
  // format: off
  // scalastyle:off line.size.limit
  type ObjectRow = ((Option[ObjectId], MuseumId, String, Option[Long], Option[String], Option[Long], Option[Long], Boolean, String, Option[String], Option[Long], Option[Int]))
  type LocalObjectRow = ((ObjectId, EventId, StorageNodeDatabaseId, MuseumId, Option[String]))
  type StorageNodeRow = ((Option[StorageNodeDatabaseId], String, String, Option[Double], Option[Double], Option[StorageNodeDatabaseId], Option[Double], Option[Double], Option[String], Option[String], Boolean, MuseumId, NodePath))
  // format: on
  // scalastyle:on line.size.limit

  val objTable    = TableQuery[ObjectTable]
  val locObjTable = TableQuery[LocalObjectsTable]
  val nodeTable   = TableQuery[StorageNodeTable]

  /**
   * Definition for the MUSIT_MAPPING.MUSITTHING table
   */
  class ObjectTable(
      val tag: Tag
  ) extends Table[ObjectRow](tag, Some("MUSIT_MAPPING"), "MUSITTHING") {

    // scalastyle:off method.name
    def * = (
      id.?,
      museumId,
      museumNo,
      museumNoAsNumber,
      subNo,
      subNoAsNumber,
      mainObjectId,
      isDeleted,
      term,
      oldSchema,
      oldObjId,
      newCollectionId
    )

    // scalastyle:on method.name

    val id               = column[ObjectId]("OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val museumId         = column[MuseumId]("MUSEUMID")
    val museumNo         = column[String]("MUSEUMNO")
    val museumNoAsNumber = column[Option[Long]]("MUSEUMNOASNUMBER")
    val subNo            = column[Option[String]]("SUBNO")
    val subNoAsNumber    = column[Option[Long]]("SUBNOASNUMBER")
    val mainObjectId     = column[Option[Long]]("MAINOBJECT_ID")
    val isDeleted        = column[Boolean]("IS_DELETED")
    val term             = column[String]("TERM")
    val oldSchema        = column[Option[String]]("OLD_SCHEMANAME")
    val oldObjId         = column[Option[Long]]("LOKAL_PK")
    val oldBarcode       = column[Option[Long]]("OLD_BARCODE")
    val newCollectionId  = column[Option[Int]]("NEW_COLLECTION_ID")

  }

  /**
   * Definition for the MUSARK_STORAGE.LOCAL_OBJECT table
   */
  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObjectRow](tag, Some("MUSARK_STORAGE"), "LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * = (
      objectId,
      latestMoveId,
      currentLocationId,
      museumId,
      objectType
    )

    // scalastyle:on method.name

    val objectId          = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId      = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeDatabaseId]("CURRENT_LOCATION_ID")
    val museumId          = column[MuseumId]("MUSEUM_ID")
    val objectType        = column[Option[String]]("OBJECT_TYPE")
  }

  /**
   * Definition for the MUSARK_STORAGE.STORAGE_NODE table
   */
  class StorageNodeTable(
      val tag: Tag
  ) extends Table[StorageNodeRow](tag, Some("MUSARK_STORAGE"), "STORAGE_NODE") {
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

    // scalastyle:off line.size.limit
    val id          = column[StorageNodeDatabaseId]("STORAGE_NODE_ID", O.PrimaryKey, O.AutoInc)
    val storageType = column[String]("STORAGE_TYPE")
    val name        = column[String]("STORAGE_NODE_NAME")
    val area        = column[Option[Double]]("AREA")
    val areaTo      = column[Option[Double]]("AREA_TO")
    val isPartOf    = column[Option[StorageNodeDatabaseId]]("IS_PART_OF")
    val height      = column[Option[Double]]("HEIGHT")
    val heightTo    = column[Option[Double]]("HEIGHT_TO")
    val groupRead   = column[Option[String]]("GROUP_READ")
    val groupWrite  = column[Option[String]]("GROUP_WRITE")
    val isDeleted   = column[Boolean]("IS_DELETED")
    val museumId    = column[MuseumId]("MUSEUM_ID")
    val path        = column[NodePath]("NODE_PATH")
    // scalastyle:on line.size.limit
  }

}
