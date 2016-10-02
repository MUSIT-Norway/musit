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

package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain._
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 7/6/16.
 */

object EventActorsDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventActorsTable = TableQuery[EventActorsTable]

  def insertActors(newEventId: Long, relatedActors: Seq[ActorWithRole]): DBIO[Option[Int]] = {
    val relActors = relatedActors.map {
      _.toEventRoleActor(newEventId)
    }
    EventActorsTable ++= relActors
  }

  def getRelatedActors(eventId: Long): Future[Seq[ActorWithRole]] = {
    val query = EventActorsTable.filter(evt => evt.eventId === eventId)
    val futEventRoleActorSeq = db.run(query.result)
    futEventRoleActorSeq.map(eventRoleActorSeq => eventRoleActorSeq.map(eventRoleActor => eventRoleActor.toActorWithRole))
  }

  private class EventActorsTable(tag: Tag) extends Table[EventRoleActor](tag, Some("MUSARK_EVENT"), "EVENT_ROLE_ACTOR") {
    def * = (eventId, roleId, actorId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val actorId = column[Int]("ACTOR_ID")

    def create = (eventId: Long, roleId: Int, actorId: Int) =>
      EventRoleActor(
        eventId,
        roleId,
        actorId
      )

    def destroy(eventRoleActor: EventRoleActor) = Some(eventRoleActor.eventId, eventRoleActor.roleId, eventRoleActor.actorId)
  }

}

