package no.uio.musit.microservice.event.dao

/**
 * Created by ellenjo on 8/22/16.
 */
/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

import no.uio.musit.microservice.event.domain.{EventRoleObject, EventRolePlace, ObjectWithRole}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventPlacesAsObjectsDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventPlacesAsObjectsTable = TableQuery[EventPlacesAsObjectsTable]

  def insertObjects(newEventId: Long, relatedObjects: Seq[ObjectWithRole]): DBIO[Option[Int]] = {
    val relObjectsAsPlaces = relatedObjects.map {
      objectWithRole => EventRolePlace(newEventId, objectWithRole.roleId, objectWithRole.objectId.toInt)
    }
    EventPlacesAsObjectsTable ++= relObjectsAsPlaces
  }

  def getRelatedObjects(eventId: Long): Future[Seq[ObjectWithRole]] = {
    val query = EventPlacesAsObjectsTable.filter(evt => evt.eventId === eventId)
    val futEventRolePlaceSeq = db.run(query.result)
    futEventRolePlaceSeq.map(eventRolePlaceSeq => eventRolePlaceSeq.map(eventRolePlace => ObjectWithRole(eventRolePlace.roleId, eventRolePlace.placeId.toLong)))
  }

  private class EventPlacesAsObjectsTable(tag: Tag) extends Table[EventRolePlace](tag, Some("MUSARK_EVENT"), "EVENT_ROLE_PLACE_AS_OBJECT") {
    def * = (eventId, roleId, placeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val placeId = column[Int]("PLACE_ID")

    def create = (eventId: Long, roleId: Int, placeId: Int) =>
      EventRolePlace(
        eventId,
        roleId,
        placeId
      )

    def destroy(eventRolePlace: EventRolePlace) = Some(eventRolePlace.eventId, eventRolePlace.roleId, eventRolePlace.placeId)
  }

}

