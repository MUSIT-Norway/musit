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

package repositories.dao.event

import com.google.inject.{Inject, Singleton}
import models.event.EventTypeId
import models.event.dto.EventRoleObject
import no.uio.musit.models.{EventId, ObjectId}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.dao.{ColumnTypeMappers, SchemaName}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class EventObjectsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val eventObjectsTable = TableQuery[EventObjectsTable]

  def insertObjects(
    eventId: EventId,
    relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjects = relatedObjects.map(_.copy(eventId = Some(eventId)))
    eventObjectsTable ++= relObjects
  }

  def getRelatedObjects(eventId: EventId): Future[Seq[EventRoleObject]] = {
    val query = eventObjectsTable.filter(_.eventId === eventId)
    db.run(query.result)
  }

  def latestEventIdsForObject(
    objectId: ObjectId,
    eventTypeId: EventTypeId,
    limit: Option[Int] = None
  ): Future[Seq[EventId]] = {
    val q = eventObjectsTable.filter { erp =>
      erp.objectId === objectId && erp.eventTypeId === eventTypeId
    }.sortBy(_.eventId.desc).map(_.eventId)

    val query = limit.map {
      case l: Int if l > 0 => q.take(l)
      case l: Int if l == -1 => q
      case l: Int => q.take(50)
    }.getOrElse(q).result
    db.run(query)
  }

  private class EventObjectsTable(
      tag: Tag
  ) extends Table[EventRoleObject](tag, SchemaName, "EVENT_ROLE_OBJECT") {

    def * = (eventId.?, roleId, objectId, eventTypeId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[EventId]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val objectId = column[ObjectId]("OBJECT_ID")
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")

    def create = (eventId: Option[EventId], roleId: Int, objectId: ObjectId, eventTypeId: EventTypeId) =>
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
