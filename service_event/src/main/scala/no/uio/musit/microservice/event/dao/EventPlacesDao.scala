package no.uio.musit.microservice.event.dao

/**
 * Created by sveigl on 8/24/16.
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

import no.uio.musit.microservice.event.domain.{ EventRolePlace, PlaceWithRole }
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventPlacesDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventPlacesTable = TableQuery[EventPlacesTable]

  def insertPlaces(newEventId: Long, relatedPlaces: Seq[PlaceWithRole]): DBIO[Option[Int]] = {
    val relPlaces = relatedPlaces.map {
      _.toEventRolePlace(newEventId)
    }
    EventPlacesTable ++= relPlaces
  }

  def getRelatedPlaces(eventId: Long): Future[Seq[PlaceWithRole]] = {
    val query = EventPlacesTable.filter(evt => evt.eventId === eventId)
    val futEventRolePlaceSeq = db.run(query.result)
    futEventRolePlaceSeq.map(eventRolePlaceSeq => eventRolePlaceSeq.map(eventRolePlace => eventRolePlace.toPlaceWithRole))
  }

  private class EventPlacesTable(tag: Tag) extends Table[EventRolePlace](tag, Some("MUSARK_EVENT"), "EVENT_ROLE_PLACE") {
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

