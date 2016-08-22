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

import no.uio.musit.microservice.event.domain.{ EventRoleObject, ObjectWithRole }
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventObjectsDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventObjectsTable = TableQuery[EventObjectsTable]

  def insertObjects(newEventId: Long, relatedObjects: Seq[ObjectWithRole]): DBIO[Option[Int]] = {
    val relObjects = relatedObjects.map {
      _.toEventRoleObject(newEventId)
    }
    EventObjectsTable ++= relObjects
  }

  def getRelatedObjects(eventId: Long): Future[Seq[ObjectWithRole]] = {
    val query = EventObjectsTable.filter(evt => evt.eventId === eventId)
    val futEventRoleObjectSeq = db.run(query.result)
    futEventRoleObjectSeq.map(eventRoleObjectSeq => eventRoleObjectSeq.map(eventRoleObject => eventRoleObject.toObjectWithRole))
  }

  private class EventObjectsTable(tag: Tag) extends Table[EventRoleObject](tag, Some("MUSARK_EVENT"), "EVENT_ROLE_OBJECT") {
    def * = (eventId, roleId, objectId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val objectId = column[Int]("OBJECT_ID")

    def create = (eventId: Long, roleId: Int, objectId: Int) =>
      EventRoleObject(
        eventId,
        roleId,
        objectId
      )

    def destroy(eventRoleObject: EventRoleObject) = Some(eventRoleObject.eventId, eventRoleObject.roleId, eventRoleObject.objectId)
  }

}

