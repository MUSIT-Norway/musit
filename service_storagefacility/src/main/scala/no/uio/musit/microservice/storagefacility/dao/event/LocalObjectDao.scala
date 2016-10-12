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
import no.uio.musit.microservice.storagefacility.dao.{ColumnTypeMappers, SchemaName}
import no.uio.musit.microservice.storagefacility.domain.{MuseumId, ObjectId}
import no.uio.musit.microservice.storagefacility.domain.event.EventId
import no.uio.musit.microservice.storagefacility.domain.event.dto.{EventDto, LocalObject}
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

class LocalObjectDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val localObjectsTable = TableQuery[LocalObjectsTable]

  private def upsert(lo: LocalObject): DBIO[Int] =
    localObjectsTable.insertOrUpdate(lo)

  def cacheLatestMove(mid: MuseumId, eventId: EventId, moveEvent: EventDto): DBIO[Int] = {
    val relObj = moveEvent.relatedObjects.headOption
    val relPlc = moveEvent.relatedPlaces.headOption

    relObj.flatMap { obj =>
      relPlc.map { place =>
        upsert(LocalObject(obj.objectId, eventId, place.placeId, mid))
      }
    }.getOrElse(
      throw new AssertionError("A MoveObject event requires both the " + // scalastyle:ignore
        "'affectedThing' and 'to' attributes set")
    )
  }

  def currentLocation(objectId: ObjectId): Future[Option[StorageNodeId]] = {
    val query = localObjectsTable.filter { locObj =>
      locObj.objectId === objectId
    }.map(_.currentLocationId).max.result

    db.run(query)
  }

  private class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObject](tag, SchemaName, "LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * = (
      objectId,
      latestMoveId,
      currentLocationId,
      museumId
    ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val objectId = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeId]("CURRENT_LOCATION_ID")
    val museumId = column[MuseumId]("MUSEUM_ID")

    def create = (
      objectId: ObjectId,
      latestMoveId: EventId,
      currentLocationId: StorageNodeId,
      museumId: MuseumId
    ) =>
      LocalObject(
        objectId,
        latestMoveId,
        currentLocationId,
        museumId
      )

    def destroy(localObject: LocalObject) =
      Some((
        localObject.objectId,
        localObject.latestMoveId,
        localObject.currentLocationId,
        localObject.museumId
      ))
  }

}
