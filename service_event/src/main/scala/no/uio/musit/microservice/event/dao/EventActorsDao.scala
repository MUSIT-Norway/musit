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

import no.uio.musit.microservice.event.dao.EventDao._
import no.uio.musit.microservice.event.domain.{ EventRelations, EventType, _ }
import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, _ }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import no.uio.musit.microservices.common.utils.Misc._
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import slick.dbio.SequenceAction

/**
 * Created by jstabel on 7/6/16.
 */

object EventActorsDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventActorsTable = TableQuery[EventActorsTable]

  def insertActors(newEventId: Long, relatedActors: Seq[EventRoleActor]): DBIO[Option[Int]] = {
    val relActors = relatedActors.map {
      case total: TotalEventRoleActor => total
      case partial: PartialEventRoleActor => partial.toTotal(newEventId)
    }
    EventActorsTable ++= relActors
  }

  def getRelatedActors(eventId: Long): Future[Seq[EventRoleActor]] = {
    val query = EventActorsTable.filter(evt => evt.eventId === eventId)
    db.run(query.result)
  }

  /*
  def insertEventLinkAction(eventLink: EventLink): DBIO[Int] = {
    insertEventLinkDtoAction(eventLink.toNormalizedEventLinkDto)
  }

  def insertEventLinkDtoAction(eventLink: EventLinkDto): DBIO[Int] = {
    //println(s"inserLink: from: ${eventLink.idFrom} relation: ${eventLink.relationId} to: ${eventLink.idTo}")
    EventLinkTable += eventLink
  }

  def getRelatedEventDtos(parentId: Long) /*: Future[Seq[(Int, BaseEventDto)]]*/ = {
    val relevantRelations = EventLinkTable.filter(evt => evt.idFrom === parentId)

    //println(s"gets relatedEventDtos for parentId: $parentId")
    val query = for {
      (eventBaseTable, relationTable) <- EventBaseTable join relevantRelations on (_.id === _.idTo)
    } yield (relationTable.relationId, eventBaseTable)

    db.run(query.result)

  }

  case class PartialEventLink(idFrom: Long, relation: EventRelation) {
    def toFullLink(idTo: Long) = EventLink(idFrom, relation, idTo)
  }

  case class EventLink(idFrom: Long, relation: EventRelation, idTo: Long) {
    def normalizedDirection = if (relation.isNormalized)
      this
    else
      EventLink(idTo, relation.getNormalizedDirection, idFrom)

    def toEventLinkDto = EventLinkDto(idFrom, relation.id, idTo)

    def toNormalizedEventLinkDto = normalizedDirection.toEventLinkDto
  }

  case class EventLinkDto(idFrom: Long, relationId: Int, idTo: Long) {
  }

*/
  private class EventActorsTable(tag: Tag) extends Table[TotalEventRoleActor](tag, Some("MUSARK_EVENT"), "EVENT_ROLE_ACTOR") {
    def * = (eventId, roleId, actorId) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Long]("EVENT_ID")
    val roleId = column[Int]("ROLE_ID")
    val actorId = column[Int]("ACTOR_ID")

    def create = (eventId: Long, roleId: Int, actorId: Int) =>
      TotalEventRoleActor(
        eventId,
        roleId,
        actorId
      )

    def destroy(eventRoleActor: TotalEventRoleActor) = Some(eventRoleActor.eventID, eventRoleActor.roleID, eventRoleActor.actorID)
  }

}

