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
import no.uio.musit.microservice.storagefacility.domain.event.dto.{ EventRoleObject, EventRolePlace }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

class EventPlacesAsObjectsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val EventPlacesAsObjectsTable = TableQuery[EventPlacesAsObjectsTable]

  def insertObjects(
    eventId: Long,
    relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjectsAsPlaces = relatedObjects.map { ero =>
      EventRolePlace(Some(eventId), ero.roleId, ero.objectId.toInt)
    }
    EventPlacesAsObjectsTable ++= relObjectsAsPlaces
  }

  def getRelatedObjects(eventId: Long): Future[Seq[EventRoleObject]] = {
    val query = EventPlacesAsObjectsTable.filter(evt => evt.eventId === eventId)
    db.run(query.result).map { places =>
      places.map { place =>
        EventRoleObject(place.eventId, place.roleId, place.placeId.toLong)
      }
    }

  }

  private class EventPlacesAsObjectsTable(
      val tag: Tag
  ) extends Table[EventRolePlace](tag, SchemaName, "EVENT_ROLE_PLACE_AS_OBJECT") {
    def * = (eventId.?, roleId, placeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val placeId = column[Int]("PLACE_ID")

    def create = (eventId: Option[Long], roleId: Int, placeId: Int) =>
      EventRolePlace(
        eventId = eventId,
        roleId = roleId,
        placeId = placeId
      )

    def destroy(eventRolePlace: EventRolePlace) =
      Some((
        eventRolePlace.eventId,
        eventRolePlace.roleId,
        eventRolePlace.placeId
      ))
  }

}
