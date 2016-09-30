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

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.dao.{ ColumnTypeMappers, SchemaName }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeId
import no.uio.musit.microservice.storagefacility.domain.event.dto.EventRoleObject
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class EventObjectsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val eventObjectsTable = TableQuery[EventObjectsTable]

  def insertObjects(
    eventId: Long,
    relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjects = relatedObjects.map(_.copy(eventId = Some(eventId)))
    eventObjectsTable ++= relObjects
  }

  def getRelatedObjects(eventId: Long): Future[Seq[EventRoleObject]] = {
    val query = eventObjectsTable.filter(_.eventId === eventId)
    db.run(query.result)
  }

  private class EventObjectsTable(
      tag: Tag
  ) extends Table[EventRoleObject](tag, SchemaName, "EVENT_ROLE_OBJECT") {

    def * = (eventId.?, roleId, objectId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val objectId = column[Long]("OBJECT_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create = (eventId: Option[Long], roleId: Int, objectId: Long, eventTypeId: EventTypeId) =>
      EventRoleObject(
        eventId = eventId,
        roleId = roleId,
        objectId = objectId,
        eventTypeId = eventTypeId
      )

    def destroy(eventRoleObject: EventRoleObject) =
      Some((
        eventRoleObject.eventId,
        eventRoleObject.roleId,
        eventRoleObject.objectId,
        eventRoleObject.eventTypeId
      ))
  }

}
