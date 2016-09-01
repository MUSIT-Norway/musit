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

package no.uio.musit.microservice.storagefacility.dao.event

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.ColumnTypeMappers
import no.uio.musit.microservice.storagefacility.dao.event.EventLinks.PartialEventLink
import no.uio.musit.microservice.storagefacility.domain.event.dto._
import no.uio.musit.microservice.storagefacility.domain.event.{EventType, MusitEvent}
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, _}
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.Effect.{All, Transactional}
import slick.dbio.{DBIOAction, SequenceAction}

import scala.concurrent.Future

class EventDao @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  val eventLinkDao: EventLinkDao
) extends SharedEventTables with ColumnTypeMappers {

  import driver.api._

  implicit lazy val libraryItemMapper =
    MappedColumnType.base[EventType, Int](
      eventType => eventType.registeredEventId.underlying,
      id => EventType.fromInt(id)
    )

  private val EventBaseTable = TableQuery[EventBaseTable]

  /**
   *
   * @param eventBaseDto
   * @return
   */
  def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[Long] =
    EventBaseTable returning EventBaseTable.map(_.id) += eventBaseDto

  // FIXME: Links are to be removed, rendering this function useless...
  private[this] def copyEventIdIntoLinks(dto: Dto, newId: Long): Seq[Link] =
    dto.links.getOrElse(Seq.empty).map { l =>
      l.copy(localTableId = Some(newId))
    }

  // FIXME: Links are to be removed, rendering this function useless...
  private[this] def insertBaseAndLinksAction(
    baseEventDto: BaseEventDto,
    partOfParent: Option[Long]
  ): DBIO[Long] = {
    (for {
      newEventId <- insertBaseAction(baseEventDto.copy(partOf = partOfParent))
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(baseEventDto, newEventId))
      _ <- LinkDao.insertLinkAction(selfLink(newEventId))
    } yield newEventId).transactionally
  }

  /**
   * Creates an action to insert the event and potentially all related
   * sub-events. PartialEventLink is used if the event is inserted as a
   * subElement of a parent element. The id of the parent element is then in
   * the partialEventLink.
   */
  // scalastyle:off
  private[this] def insertEventAction(
    event: Dto,
    partialEventLink: Option[PartialEventLink],
    recursive: Boolean
  ): DBIO[Long] = {

    val parentId = partialEventLink.map(_.idFrom)

    // We want partOfParent to be Some(parentId) if event has a parent and that
    // parent is in the parts-relation to us.
    val isPartsRelation = partialEventLink.exists(_.relation == EventRelations.relation_parts)

    val partOfParent = partialEventLink.filter(_ => isPartsRelation).map(_.idFrom)

    val insertBaseAndLinksAction = insertBaseAndLinksAction

    event match {
      case simple: BaseEventDto =>
        insertBaseAndLinksAction

      case complex: ExtendedDto =>
        (for {
          newEventId <- insertBaseAndLinksAction
          numInserted <- complex.createInsertCustomDtoAction(newEventId, event)
        } yield newEventId).transactionally
        ???
    }

    var action = event.eventType.maybeMultipleTables.fold(insertBaseAndLinksAction) {
      complexEventType =>
        (for {
          newEventId <- insertBaseAndLinksAction
          numInserted <- complexEventType.createInsertCustomDtoAction(newEventId, event)
        } yield newEventId).transactionally
    }
    if (recursive && event.relatedSubEvents.nonEmpty) {
      action = (for {
        newEventId <- action
        numInserted <- insertChildrenAction(newEventId, event)
      } yield newEventId).transactionally

    }

    if (!isPartsRelation && partialEventLink.isDefined) {
      // The partOf-relation is stored in the main event table, so this is only
      // used for the other relations (stored in the EVENT_RELATION_EVENT table).
      action = (for {
        newEventId <- action
        numInserted <- eventLinkDao.insertEventLinkAction(partialEventLink.get.toFullLink(newEventId))
      } yield newEventId).transactionally
    }
    action
  }

  def insertEvent(event: MusitEvent, recursive: Boolean): Future[Long] = {
    val action = insertEventAction(event, None, recursive)
    db.run(action)
  }

  def insertChildrenAction(parentEventId: Long, parentEvent: MusitEvent) = {

    def insertRelatedEvents(relatedEvents: RelatedEvents) = {
      val actions = relatedEvents.events.map { subEvent =>
        insertEventAction(subEvent, Some(PartialEventLink(parentEventId, relatedEvents.relation)), true)
      } //Todo, also handle other relations than parts

      SequenceAction(actions.toIndexedSeq)
    }

    val actions = parentEvent.relatedSubEvents.map(relatedEvents => insertRelatedEvents(relatedEvents))
    SequenceAction(actions.toIndexedSeq)
  }

  def getBaseEvent(id: Long): Future[Option[BaseEventDto]] = {
    val action = EventBaseTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private def createEventInMemory(baseEventDto: BaseEventDto, relatedSubEvents: Seq[RelatedEvents]): MusitFuture[MusitEvent] = {
    val id = baseEventDto.id.getOrFail("Internal error, id missing") // FIXME: This is extremely hypothetical and a compltely unnecessary check.
    val baseProps = baseEventDto.copy(relatedSubEvents = relatedSubEvents)
    baseEventDto.eventType.eventImplementation match {
      case singleTableEventType: SingleTableEventType =>
        MusitFuture.successful(singleTableEventType.createEventInMemory(baseProps))

      case multipleTablesEventType: MultipleTablesEventType =>
        multipleTablesEventType.getEventFromDatabase(id, baseProps)
    }
  }

  private def getEvent(baseEventDto: BaseEventDto, recursive: Boolean): MusitFuture[MusitEvent] = {
    val id = baseEventDto.id.getOrFail("Internal error, id missing")

    val futureSubEvents =
      if (recursive) {
        getSubEvents(id, recursive)
      } else {
        MusitFuture.successful(Seq.empty[RelatedEvents])
      }

    futureSubEvents.musitFutureFlatMap(subEvents => createEventInMemory(baseEventDto, subEvents))
  }

  def getEvent(id: Long, recursive: Boolean): MusitFuture[MusitEvent] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture(ErrorHelper.badRequest(s"Event with id: $id not found"))

    maybeBaseEventDto.musitFutureFlatMap { baseEventDto =>
      getEvent(baseEventDto, recursive)
    }
  }

  /**
   * Gets the sub-events from the event with id=parentId from the database.
   * Embedded in the proper relations (RelatedEvents-objects)
   */
  //noinspection ScalaStyle
  def getSubEvents(parentId: Long, recursive: Boolean): MusitFuture[Seq[RelatedEvents]] = {

    def getEventsFromEventDtos(eventDtos: MusitFuture[Seq[BaseEventDto]]): MusitFuture[Seq[MusitEvent]] = {
      eventDtos.musitFutureFlatMap {
        subEventDtos: Seq[BaseEventDto] =>
          MusitFuture.traverse(subEventDtos) { subEventDto: BaseEventDto => getEvent(subEventDto, recursive) }
      }
    }

    /** Gets the base event data for the children (parts) of the event with id = parentId */
    def getSubEventDtos(parentId: Long): Future[Seq[BaseEventDto]] = {
      val action = EventBaseTable.filter(event => event.partOf === parentId).result
      db.run(action)
    }

    //The parts/partOf relation
    def getPartEvents: MusitFuture[RelatedEvents] = {
      val futureSubEventDtos = getSubEventDtos(parentId).toMusitFuture
      //Create a parts-relation of the these subEvents
      getEventsFromEventDtos(futureSubEventDtos).musitFutureMap(events => RelatedEvents(EventRelations.relation_parts, events))
    }

    //The "other" relations, those stored in the event_relation_event table
    def getOtherRelatedEvents: MusitFuture[Seq[RelatedEvents]] = {
      def transfromGroup(group: Seq[(Int, BaseEventDto)]): MusitFuture[Seq[MusitEvent]] = {
        val groupWithoutRelationIdInt = group.map(_._2)
        val groupOfMusitResultEvents = groupWithoutRelationIdInt.map(dto => getEvent(dto, recursive))

        //Collapse the inner musitresults...
        val result = MusitFuture.traverse[MusitFuture[MusitEvent], MusitEvent](groupOfMusitResultEvents)(identity)
        result
      }

      val futureRelatedDtos = eventLinkDao.getRelatedEventDtos(parentId).toMusitFuture

      futureRelatedDtos.musitFutureFlatMap { relatedDtos =>

        val groupedByRelation = relatedDtos.groupBy { case (relationId, dto) => relationId }
        val groups = groupedByRelation.map {
          case (relationId, related) =>
            (relationId, transfromGroup(related))
        }.toSeq

        //More inner MusitResult-collapsing
        val result = MusitFuture.traverse(groups) {
          case (relId, futureEvents) =>
            futureEvents.musitFutureMap { events =>
              RelatedEvents(EventRelations.getByIdOrFail(relId), events)
            }
        }
        result
      }
    }

    val futurePartsEvents = getPartEvents
    val futureOtherRelatedEvents = getOtherRelatedEvents

    //Now we simply need to join the parts-events and the other related events
    futurePartsEvents.musitFutureFlatMap { partEvents =>
      futureOtherRelatedEvents.musitFutureMap { extraRelatedEvents =>
        if (partEvents.events.isEmpty)
          extraRelatedEvents
        else
          partEvents +: extraRelatedEvents
      }
    }
  }

}
