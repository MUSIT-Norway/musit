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

package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.SchemaName
import no.uio.musit.microservice.storagefacility.domain.event.dto.{EventDto, LocalObject}
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class LocalObjectDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val localObjectsTable = TableQuery[LocalObjectsTable]

  private def upsert(lo: LocalObject): DBIO[Int] =
    localObjectsTable.insertOrUpdate(lo)

  def cacheLatestMove(eventId: Long, moveEvent: EventDto): DBIO[Int] = {
    val relObj = moveEvent.relatedObjects.headOption
    val relPlc = moveEvent.relatedPlaces.headOption

    relObj.flatMap { obj =>
      relPlc.map { place =>
        upsert(LocalObject(obj.objectId, eventId, place.placeId))
      }
    }.getOrElse(
      throw new AssertionError("A MoveObject event requires both the " + // scalastyle:ignore
        "'affectedThing' and 'to' attributes set")
    )
  }

  def currentLocation(objectId: Long): Future[Option[StorageNodeId]] = {
    val query = localObjectsTable.filter { locObj =>
      locObj.objectId === objectId
    }.map(_.currentLocationId).max.result.map(_.map(StorageNodeId.apply))

    db.run(query)
  }

  private class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObject](tag, SchemaName, "LOCAL_OBJECT") {
    def * = (
      objectId,
      latestMoveId,
      currentLocationId
    ) <> (create.tupled, destroy)

    val objectId = column[Long]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId = column[Long]("LATEST_MOVE_ID")
    val currentLocationId = column[Long]("CURRENT_LOCATION_ID")

    def create = (objectId: Long, latestMoveId: Long, currentLocationId: Long) =>
      LocalObject(
        objectId,
        latestMoveId,
        currentLocationId
      )

    def destroy(localObject: LocalObject) =
      Some((
        localObject.objectId,
        localObject.latestMoveId,
        localObject.currentLocationId
      ))
  }

}
