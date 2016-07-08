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

import no.uio.musit.microservice.event.dao.EventLinkDao.PartialEventLink
import no.uio.musit.microservice.event.domain.{ RelatedEvents, _ }
import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, _ }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.dbio.SequenceAction
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventBaseTable = TableQuery[EventBaseTable]

  def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[Long] =
    EventBaseTable returning EventBaseTable.map(_.id) += eventBaseDto

  def selfLink(id: Long) =
    LinkService.local(Some(id), "self", s"/v1/$id")

  /*Creates an action to insert the event and potentially all related subevents. PartialEventLink is used if the event is inserted as a subElement of a parent element. The id of the parent element is then in the partialEventLink.*/
  def insertEventAction(event: Event, partialEventLink: Option[PartialEventLink], recursive: Boolean): DBIO[Long] = {
    def copyEventIdIntoLinks(eventBase: Event, newId: Long) = event.links.getOrElse(Seq.empty).map(l => l.copy(localTableId = Some(newId)))

    val parentId = partialEventLink.map(_.idFrom)
    //parentId.map(pid => println(s"parentID: $pid for event type: ${event.eventType.name}"))

    //We want partOfParent to be Some(parentId) if event has a parent and that parent is in the parts-relation to us.
    val isPartsRelation = partialEventLink.fold(false)(_.relation == EventRelations.relation_parts)

    val partOfParent = partialEventLink.filter(_ => isPartsRelation).map(_.idFrom)

    val insertBaseAndLinksAction = (for {
      newEventId <- insertBaseAction(event.baseEventProps.copy(partOf = partOfParent)) //#OLD (EventHelpers.eventDtoToStoreInDatabase(event, partOfParent))
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(event, newEventId))
      _ <- LinkDao.insertLinkAction(selfLink(newEventId))
    } yield newEventId).transactionally

    var action = event.eventType.maybeMultipleTables.fold(insertBaseAndLinksAction) {
      complexEventType =>
        (for {
          newEventId <- insertBaseAndLinksAction
          numInserted <- complexEventType.createInsertCustomDtoAction(newEventId, event)
        } yield newEventId).transactionally
    }
    if (recursive && event.hasSubEvents) {
      action = (for {
        newEventId <- action
        numInserted <- insertChildrenAction(newEventId, event)
      } yield newEventId).transactionally

    }

    if (!isPartsRelation && partialEventLink.isDefined) {
      action = (for {
        newEventId <- action
        numInserted <- EventLinkDao.insertEventLinkAction(partialEventLink.get.toFullLink(newEventId))
      } yield newEventId).transactionally
    }
    action
  }

  def insertEvent(event: Event, recursive: Boolean): Future[Long] = {
    val action = insertEventAction(event, None, recursive)
    db.run(action)
  }

  def insertChildrenAction(parentEventId: Long, parentEvent: Event) = {

    def insertRelatedEvents(relatedEvents: RelatedEvents) = {
      val actions = relatedEvents.events.map(subEvent => insertEventAction(subEvent, Some(PartialEventLink(parentEventId, relatedEvents.relation)), true)) //Todo, also handle other relations than parts
      new SequenceAction(actions.toIndexedSeq)
    }

    val actions = parentEvent.relatedSubEvents.map(relatedEvents => insertRelatedEvents(relatedEvents))
    new SequenceAction(actions.toIndexedSeq)
  }

  def getBaseEvent(id: Long): Future[Option[BaseEventDto]] = {
    val action = EventBaseTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private def getSubEventDtos(parentId: Long): Future[Seq[BaseEventDto]] = {
    val action = EventBaseTable.filter(event => event.partOf === parentId).result
    db.run(action)
  }

  private def createEventInMemory(baseEventDto: BaseEventDto, relatedSubEvents: Seq[RelatedEvents]): MusitFuture[Event] = {
    val id = baseEventDto.id.getOrFail("Internal error, id missing")
    //val baseProps = baseEventDto.props(relatedSubEvents)
    val baseProps = baseEventDto.copy(relatedSubEvents = relatedSubEvents)
    baseEventDto.eventType.eventImplementation match {
      case singleTableEventType: SingleTableEventType => MusitFuture.successful(singleTableEventType.createEventInMemory(baseProps))
      case multipleTablesEventType: MultipleTablesEventType => multipleTablesEventType.getEventFromDatabase(id, baseProps)
      /*#OLD

              case singleTableSingleDto: SingleTableNotUsingCustomFields => MusitFuture.successful(singleTableSingleDto.createEventInMemory(baseProps))

      case singleTableMultipleDtos: SingleTableUsingCustomFields =>
        val customDto = singleTableMultipleDtos.baseTableToCustomDto(baseEventDto)
        MusitFuture.successful(singleTableMultipleDtos.createEventInMemory(baseProps, customDto))
      case multipleTablesMultipleDtos: MultipleTablesNotUsingCustomFields => multipleTablesMultipleDtos.getEventFromDatabase(id, baseProps)

         */
    }
  }

  private def getEvent(baseEventDto: BaseEventDto, recursive: Boolean): MusitFuture[Event] = {
    val id = baseEventDto.id.getOrFail("Internal error, id missing")

    val futureSubEvents =
      if (recursive) {
        getSubEvents(id, recursive)

      } else
        MusitFuture.successful(Seq.empty[RelatedEvents])

    futureSubEvents.musitFutureFlatMap(subEvents => createEventInMemory(baseEventDto, subEvents))
  }

  def getEvent(id: Long, recursive: Boolean): MusitFuture[Event] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture(ErrorHelper.badRequest(s"Event with id: $id not found"))

    maybeBaseEventDto.musitFutureFlatMap {
      baseEventDto => getEvent(baseEventDto, recursive)
    }
  }

  /* Gets the subevents from of the event with id=parentId from the database. Embedded in the proper relations (RelatedEvents-objects)*/

  def getSubEvents(parentId: Long, recursive: Boolean): MusitFuture[Seq[RelatedEvents]] = {

    def getEventsFromEventDtos(eventDtos: MusitFuture[Seq[BaseEventDto]]): MusitFuture[Seq[Event]] = {
      eventDtos.musitFutureFlatMap {
        subEventDtos: Seq[BaseEventDto] =>
          MusitFuture.traverse(subEventDtos) { subEventDto: BaseEventDto => getEvent(subEventDto, recursive) }
      }
    }

    //The parts/partOf relation
    def getPartEvents: MusitFuture[RelatedEvents] = {
      val futureSubEventDtos = getSubEventDtos(parentId).toMusitFuture
      //Create a parts-relation of the these subEvents
      getEventsFromEventDtos(futureSubEventDtos).musitFutureMap(events => RelatedEvents(EventRelations.relation_parts, events))
    }

    //The "other" relations, those stored in the event_relation_event table
    def getOtherRelatedEvents: MusitFuture[Seq[RelatedEvents]] = {
      def transfromGroup(group: Seq[(Int, BaseEventDto)]): MusitFuture[Seq[Event]] = {
        val groupWithoutRelationIdInt = group.map(_._2)
        val groupOfMusitResultEvents = groupWithoutRelationIdInt.map(dto => getEvent(dto, recursive))

        //Collapse the inner musitresults...
        val result = MusitFuture.traverse(groupOfMusitResultEvents)(identity)
        result
      }

      val futureRelatedDtos = EventLinkDao.getRelatedEventDtos(parentId).toMusitFuture

      futureRelatedDtos.musitFutureFlatMap { relatedDtos =>

        val groupedByRelation = relatedDtos.groupBy { case (relationId, dto) => relationId }
        val groups = groupedByRelation.map { case (relationId, related) => (relationId, transfromGroup(related)) }.toSeq

        //More inner MusitResult-collapsing
        val result = MusitFuture.traverse(groups) {
          case (relId, futureEvents) =>
            futureEvents.musitFutureMap { events => RelatedEvents(EventRelations.getByIdOrFail(relId), events) }
        }
        result
      }
    }

    val futurePartsEvents = getPartEvents
    val futureOtherRelatedEvents = getOtherRelatedEvents

    //Now we simply need to join the parts-events and the other related events
    futurePartsEvents.musitFutureFlatMap {
      partEvents =>
        futureOtherRelatedEvents.musitFutureMap {
          extraRelatedEvents =>
            if (partEvents.events.isEmpty)
              extraRelatedEvents
            else
              partEvents +: extraRelatedEvents
        }
    }
  }

  implicit lazy val libraryItemMapper = MappedColumnType.base[EventType, Int](
    eventType => eventType.id,
    id => EventType.getById(id)
  )

  class EventBaseTable(tag: Tag) extends Table[BaseEventDto](tag, Some("MUSARK_EVENT"), "EVENT") {
    def * = (id.?, eventTypeID, eventNote, partOf, valueLong, valueString) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeID = column[EventType]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    val partOf = column[Option[Long]]("PART_OF")
    val valueLong = column[Option[Long]]("VALUE_LONG")
    val valueString = column[Option[String]]("VALUE_STRING")

    def create = (id: Option[Long], eventType: EventType, note: Option[String], partOf: Option[Long], valueLong: Option[Long], valueString: Option[String]) =>
      BaseEventDto(
        id,
        Some(Seq(selfLink(id.getOrFail("EventBaseTable internal error")))),
        eventType,
        note,
        Seq.empty,
        partOf,
        valueLong,
        valueString
      )

    def destroy(event: BaseEventDto) = Some(event.id, event.eventType, event.note, event.partOf, event.valueLong, event.valueString)
  }

}
