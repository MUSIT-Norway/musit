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

object EventDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventBaseTable = TableQuery[EventBaseTable]

  def insertBaseAction(eventBaseDto: EventBaseDto): DBIO[Long] =
    EventBaseTable returning EventBaseTable.map(_.id) += eventBaseDto

  def selfLink(id: Long) =
    LinkService.local(Some(id), "self", s"/v1/$id")

  def insertEvent(event: Event): Future[Long] = {
    def copyEventIdIntoLinks(eventBase: Event, newId: Long) = event.links.getOrElse(Seq.empty).map(l => l.copy(localTableId = Some(newId)))

    val insertBaseAndLinksAction = (for {
      newEventId <- insertBaseAction(EventBaseDto.fromEvent(event))
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(event, newEventId))
      _ <- selfLink(newEventId) |> LinkDao.insertLinkAction
    } yield newEventId).transactionally

    val combinedAction = event.eventType.eventFactory.maybeActionCreator.fold(insertBaseAndLinksAction) {
      actionCreator =>
        (for {
          newEventId <- insertBaseAndLinksAction
          numInserted <- actionCreator(newEventId, event)
        } yield newEventId).transactionally
    }

    db.run(combinedAction)

  }

  def getBaseEvent(id: Long): Future[Option[EventBaseDto]] = {
    val action = EventBaseTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  def getEvent(id: Long): MusitFuture[Event] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture(ErrorHelper.badRequest(s"Event with id: $id not found"))

    maybeBaseEventDto.musitFutureFlatMap {
      baseEventDto => baseEventDto.eventType.eventFactory.fromDatabase(id, baseEventDto)
    }
  }

  case class EventBaseDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String],
      valueLong: Option[Long] = None) {

    def valueLongToOptBool = valueLong match {
      case Some(1) => Some(true)
      case Some(0) => Some(false)
      case None => None
      case n => throw new MusitInternalErrorException(s"Wrong boolean value $n")
    }

    def valueLongToBool = valueLongToOptBool match {
      case Some(b) => b
      case None => throw new MusitInternalErrorException("Missing boolean value")
    }
  }
  object EventBaseDto {
    def fromEvent(evt: Event) = EventBaseDto(evt.id, evt.links, evt.eventType, evt.note)
  }

  implicit lazy val libraryItemMapper = MappedColumnType.base[EventType, Int](
    eventType => eventType.id,
    id => EventType.getById(id)
  )

  private class EventBaseTable(tag: Tag) extends Table[EventBaseDto](tag, Some("MUSARK_EVENT"), "EVENT") {
    def * = (id.?, eventTypeID, eventNote) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeID = column[EventType]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    def create = (id: Option[Long], eventType: EventType, note: Option[String]) =>
      EventBaseDto(
        id,
        Some(Seq(selfLink(id.getOrFail("EventBaseTable internal error")))),
        eventType,
        note
      )

    def destroy(event: EventBaseDto) = Some(event.id, event.eventType, event.note)
  }

}
