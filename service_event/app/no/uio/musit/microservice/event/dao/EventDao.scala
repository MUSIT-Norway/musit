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

import no.uio.musit.microservice.event.domain.{ BaseEventProps, _ }
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

object EventDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventBaseTable = TableQuery[EventBaseTable]

  def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[Long] =
    EventBaseTable returning EventBaseTable.map(_.id) += eventBaseDto

  def selfLink(id: Long) =
    LinkService.local(Some(id), "self", s"/v1/$id")

  def insertEvent(event: Event): Future[Long] = {
    def copyEventIdIntoLinks(eventBase: Event, newId: Long) = event.links.getOrElse(Seq.empty).map(l => l.copy(localTableId = Some(newId)))

    val insertBaseAndLinksAction = (for {
      newEventId <- insertBaseAction(EventHelpers.eventDtoToStoreInDatabase(event))
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(event, newEventId))
      _ <- selfLink(newEventId) |> LinkDao.insertLinkAction
    } yield newEventId).transactionally

    val combinedAction = event.eventType.maybeMultipleTablesMultipleDtos.fold(insertBaseAndLinksAction) {
      complexEventType =>
        (for {
          newEventId <- insertBaseAndLinksAction
          numInserted <- complexEventType.createInsertCustomDtoAction(newEventId, event)
        } yield newEventId).transactionally
    }

    db.run(combinedAction)

  }

  def getBaseEvent(id: Long): Future[Option[BaseEventDto]] = {
    val action = EventBaseTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  def getEvent(id: Long): MusitFuture[Event] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture(ErrorHelper.badRequest(s"Event with id: $id not found"))

    maybeBaseEventDto.musitFutureFlatMap {
      baseEventDto =>
        baseEventDto.eventType.eventImplementation match {

          case singleTableSingleDto: SingleTableSingleDto => MusitFuture.successful(singleTableSingleDto.createEventInMemory(baseEventDto.props))

          case singleTableMultipleDtos: SingleTableMultipleDtos =>
            val customDto = singleTableMultipleDtos.baseTableToCustomDto(baseEventDto)
            MusitFuture.successful(singleTableMultipleDtos.createEventInMemory(baseEventDto.props, customDto))
          case multipleTablesMultipleDtos: MultipleTablesMultipleDtos => multipleTablesMultipleDtos.getEventFromDatabase(id, baseEventDto)
        }
    }
  }

  case class BaseEventDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String],
      valueLong: Option[Long] = None) {

    def getOptBool = valueLong match {
      case Some(1) => Some(true)
      case Some(0) => Some(false)
      case None => None
      case n => throw new MusitInternalErrorException(s"Wrong boolean value $n")
    }

    def getBool = getOptBool match {
      case Some(b) => b
      case None => throw new MusitInternalErrorException("Missing boolean value")
    }

    private def boolToLong(bool: Boolean) = if (bool) 1 else 0

    def setBool(value: Boolean) = this.copy(valueLong = Some(boolToLong(value)))

    def props = BaseEventProps.fromBaseEventDto(this)

  }

  /*#OLD
  object BaseEventDto {
    def fromEvent(evt: Event) = evt.fromEventToCustomBaseData(BaseEventDto(evt.id, evt.links, evt.eventType, evt.note))
  }
  */

  implicit lazy val libraryItemMapper = MappedColumnType.base[EventType, Int](
    eventType => eventType.id,
    id => EventType.getById(id)
  )

  private class EventBaseTable(tag: Tag) extends Table[BaseEventDto](tag, Some("MUSARK_EVENT"), "EVENT") {
    def * = (id.?, eventTypeID, eventNote, valueLong) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeID = column[EventType]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    val valueLong = column[Option[Long]]("VALUE_LONG")

    def create = (id: Option[Long], eventType: EventType, note: Option[String], valueLong: Option[Long]) =>
      BaseEventDto(
        id,
        Some(Seq(selfLink(id.getOrFail("EventBaseTable internal error")))),
        eventType,
        note,
        valueLong
      )

    def destroy(event: BaseEventDto) = Some(event.id, event.eventType, event.note, event.valueLong)
  }

}
