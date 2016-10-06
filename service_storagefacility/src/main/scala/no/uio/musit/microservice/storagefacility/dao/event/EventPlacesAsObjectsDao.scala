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

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.dao.{ColumnTypeMappers, SchemaName}
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeId
import no.uio.musit.microservice.storagefacility.domain.event.dto.{EventRoleObject, EventRolePlace}
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class EventPlacesAsObjectsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  val logger = Logger(classOf[EventPlacesAsObjectsDao])

  import driver.api._

  private val placesAsObjectsTable = TableQuery[EventPlacesAsObjectsTable]

  def insertObjects(
    eventId: Long,
    relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjectsAsPlaces = relatedObjects.map { ero =>
      EventRolePlace(Some(eventId), ero.roleId, ero.objectId, ero.eventTypeId)
    }
    placesAsObjectsTable ++= relObjectsAsPlaces
  }

  def getRelatedObjects(eventId: Long): Future[Seq[EventRoleObject]] = {
    val query = placesAsObjectsTable.filter(_.eventId === eventId)
    db.run(query.result).map { places =>
      logger.debug(s"Found ${places.size} places")
      places.map { place =>
        EventRoleObject(place.eventId, place.roleId, place.placeId, place.eventTypeId)
      }
    }
  }

  def latestEventIdFor(
    nodeId: StorageNodeId,
    eventTypeId: EventTypeId
  ): Future[Option[Long]] = {
    val queryMax = placesAsObjectsTable.filter { erp =>
      erp.placeId === nodeId && erp.eventTypeId === eventTypeId
    }.map(_.eventId).max.result

    db.run(queryMax)
  }

  def latestEventIdsFor(
    nodeId: StorageNodeId,
    eventTypeId: EventTypeId,
    limit: Option[Int] = None
  ): Future[Seq[Long]] = {
    val q = placesAsObjectsTable.filter { erp =>
      erp.placeId === nodeId && erp.eventTypeId === eventTypeId
    }.sortBy(_.eventId.desc).map(_.eventId)

    val query = limit.map {
      case l: Int if l > 0 => q.take(l)
      case l: Int if l == -1 => q
      case l: Int => q.take(50)
    }.getOrElse(q).result
    db.run(query)
  }

  private class EventPlacesAsObjectsTable(
      val tag: Tag
  ) extends Table[EventRolePlace](tag, SchemaName, "EVENT_ROLE_PLACE_AS_OBJECT") {
    def * = (eventId.?, roleId, placeId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val placeId = column[StorageNodeId]("PLACE_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create = (
      eventId: Option[Long],
      roleId: Int,
      placeId: StorageNodeId,
      eventTypeId: EventTypeId
    ) =>
      EventRolePlace(
        eventId = eventId,
        roleId = roleId,
        placeId = placeId,
        eventTypeId = eventTypeId
      )

    def destroy(eventRolePlace: EventRolePlace) =
      Some((
        eventRolePlace.eventId,
        eventRolePlace.roleId,
        eventRolePlace.placeId,
        eventRolePlace.eventTypeId
      ))
  }

}
