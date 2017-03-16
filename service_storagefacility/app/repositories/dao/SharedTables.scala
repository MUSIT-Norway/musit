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

import models.ObjectTypes.ObjectType
import models.event.dto.LocalObject
import no.uio.musit.models._

private[dao] trait SharedTables extends BaseDao with ColumnTypeMappers {

  import driver.api._

  val localObjectsTable = TableQuery[LocalObjectsTable]

  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObject](tag, SchemaName, "LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * =
      (
        objectId,
        latestMoveId,
        currentLocationId,
        museumId,
        objectType
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val objectId          = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId      = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeDatabaseId]("CURRENT_LOCATION_ID")
    val museumId          = column[MuseumId]("MUSEUM_ID")
    val objectType        = column[String]("OBJECT_TYPE")

    def create =
      (
          objectId: ObjectId,
          latestMoveId: EventId,
          currentLocationId: StorageNodeDatabaseId,
          museumId: MuseumId,
          objectType: String
      ) =>
        LocalObject(
          objectId = objectId,
          latestMoveId = latestMoveId,
          currentLocationId = currentLocationId,
          museumId = museumId,
          objectType = objectType
      )

    def destroy(localObject: LocalObject) =
      Some(
        (
          localObject.objectId,
          localObject.latestMoveId,
          localObject.currentLocationId,
          localObject.museumId,
          localObject.objectType
        )
      )
  }

}
