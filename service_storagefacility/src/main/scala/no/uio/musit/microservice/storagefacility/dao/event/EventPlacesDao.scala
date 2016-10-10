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
import no.uio.musit.microservice.storagefacility.domain.event.{EventId, EventTypeId}
import no.uio.musit.microservice.storagefacility.domain.event.dto.EventRolePlace
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class EventPlacesDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val eventPlacesTable = TableQuery[EventPlacesTable]

  def insertPlaces(
    eventId: EventId,
    relatedPlaces: Seq[EventRolePlace]
  ): DBIO[Option[Int]] = {
    val relPlaces = relatedPlaces.map(_.copy(eventId = Some(eventId)))
    eventPlacesTable ++= relPlaces
  }

  def getRelatedPlaces(eventId: EventId): Future[Seq[EventRolePlace]] = {
    val query = eventPlacesTable.filter(evt => evt.eventId === eventId)
    db.run(query.result)
  }

  private class EventPlacesTable(
      tag: Tag
  ) extends Table[EventRolePlace](tag, SchemaName, "EVENT_ROLE_PLACE") {

    def * = (eventId.?, roleId, placeId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[EventId]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val placeId = column[StorageNodeId]("PLACE_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create = (eventId: Option[EventId], roleId: Int, placeId: StorageNodeId, eventTypeId: EventTypeId) =>
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
