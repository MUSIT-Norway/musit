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
import no.uio.musit.microservice.storagefacility.domain.ActorId
import no.uio.musit.microservice.storagefacility.domain.event.EventId
import no.uio.musit.microservice.storagefacility.domain.event.dto.EventRoleActor
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class EventActorsDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val eventActorsTable = TableQuery[EventActorsTable]

  def insertActors(
    eventId: EventId,
    relatedActors: Seq[EventRoleActor]
  ): DBIO[Option[Int]] = {
    val relActors = relatedActors.map(_.copy(eventId = Some(eventId)))
    eventActorsTable ++= relActors
  }

  def getRelatedActors(eventId: EventId): Future[Seq[EventRoleActor]] = {
    val query = eventActorsTable.filter(evt => evt.eventId === eventId)
    db.run(query.result)
  }

  private class EventActorsTable(
      tag: Tag
  ) extends Table[EventRoleActor](tag, SchemaName, "EVENT_ROLE_ACTOR") {

    def * = (eventId.?, roleId, actorId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[EventId]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val actorId = column[ActorId]("ACTOR_ID")

    def create = (eventId: Option[EventId], roleId: Int, actorId: ActorId) =>
      EventRoleActor(
        eventId = eventId,
        roleId = roleId,
        actorId = actorId
      )

    def destroy(eventRoleActor: EventRoleActor) =
      Some((
        eventRoleActor.eventId,
        eventRoleActor.roleId,
        eventRoleActor.actorId
      ))
  }

}
