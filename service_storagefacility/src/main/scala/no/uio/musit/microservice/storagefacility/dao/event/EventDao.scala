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
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.SequenceAction

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
  ): DBIO[Long] = (
    for {
      newEventId <- insertBaseAction(baseEventDto.copy(partOf = partOfParent))
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(baseEventDto, newEventId))
      _ <- LinkDao.insertLinkAction(selfLink(newEventId))
    } yield {
      newEventId
    }
    ).transactionally

  /**
   * Helper to build up the correct insert action depending on Dto type.
   */
  private[this] def buildInsertAction(
    event: Dto,
    parentId: Option[Long]
  ): DBIO[Long] = {
    event match {
      case simple: BaseEventDto =>
        insertBaseAndLinksAction(simple, parentId)

      case complex: ExtendedDto =>
        val base = complex.baseEventDto
        insertBaseAndLinksAction(base, parentId).flatMap { eventId =>
          val custom: DBIO[Int] = complex.extension match {
            case ext: ObservationFromToDto =>
              // FIXME: When observation to from DAO is completed.
              // obsToFromDao.insert
              ???

            case ext: ObservationPestDto =>
              // FIXME: When observation pest DAO is completed.
              ???

            case ext: EnvRequirementDto =>
              // FIXME: When environment req DAO is completed.
              ???
          }
          custom.map(_ => eventId)
        }
    }
  }

  /**
   * Creates an action to insert the event and potentially all related
   * sub-events. PartialEventLink is used if the event is inserted as a
   * subElement of a parent element. The id of the parent element is then in
   * the partialEventLink.
   */
  private[this] def insertEventAction(
    event: Dto,
    partialEventLink: Option[PartialEventLink]
  ): DBIO[Long] = {

    // We want partOfParent to be Some(parentId) if event has a parent and that
    // parent is in the parts-relation to us.
    // TODO: The following two lines of code is very confusing, what is it supposed to do?
    val isPartsRelation = partialEventLink.exists(_.relation == EventRelations.relation_parts)
    val partOfParent = partialEventLink.filter(_ => isPartsRelation).map(_.idFrom)

    val insertBase = buildInsertAction(event, partOfParent)

    val insertWithChildren =
      for {
        theEventId <- insertBase
        _ <- insertChildrenAction(theEventId, event.relatedSubEvents)
        _ <- {
          partialEventLink.filterNot(_ => isPartsRelation).map { pel =>
            eventLinkDao.insertEventLinkAction(pel.toFullLink(theEventId))
          }.getOrElse(DBIO.successful[Int](0))
        }
      } yield theEventId

    insertWithChildren.transactionally
  }

  /**
   * TODO: Document me!!!
   */
  def insertEvent(event: Dto): Future[Long] = {
    val action = insertEventAction(event, None)
    db.run(action)
  }

  /**
   * TODO: Document me!!!
   */
  def insertChildrenAction(parentEventId: Long, children: Seq[RelatedEvents]) = {
    val actions = children.map { relatedEvents =>
      val relActions = relatedEvents.events.map { subEvent =>
        insertEventAction(
          subEvent,
          Some(PartialEventLink(parentEventId, relatedEvents.relation))
        )
      } //Todo, also handle other relations than parts
      SequenceAction(relActions.toIndexedSeq)
    }

    SequenceAction(actions.toIndexedSeq)
  }

  /**
   * TODO: Document me!!!
   */
  def getBaseEvent(id: Long): Future[Option[BaseEventDto]] = {
    val action = EventBaseTable.filter(_.id === id).result.headOption
    db.run(action)
  }

//  private def createEventInMemory(
//    baseEventDto: BaseEventDto,
//    relatedSubEvents: Seq[RelatedEvents]
//  ): MusitFuture[Dto] = {
//    /*
//      TODO:
//       1. remove getOrFail so we're operating on a pure Option.get.
//          Having an ID = null is not possible!
//
//       2. Identify baseEventDto registered EventType from eventTypeId.
//       3. pattern match on event type to identify which Extended DTO to use.
//       4. Call out to appropriate DAO to fetch the data
//       5. Return the Dto
//     */
//
//    // The ID field will never be None or null in this context
//    val id = baseEventDto.id.get // scalastyle:ignore
//    val baseProps = baseEventDto.copy(relatedSubEvents = relatedSubEvents)
//
//    baseEventDto.eventType.eventImplementation match {
//      case singleTableEventType: SingleTableEventType =>
//        MusitFuture.successful(singleTableEventType.createEventInMemory(baseProps))
//
//      case multipleTablesEventType: MultipleTablesEventType =>
//        multipleTablesEventType.getEventFromDatabase(id, baseProps)
//    }
//  }

  private def getEvent(baseEventDto: BaseEventDto, recursive: Boolean): MusitFuture[MusitEvent] = {
    val id = baseEventDto.id.get // This can never be None or null in this context

    val futureSubEvents =
      if (recursive) {
        getSubEvents(id, recursive)
      } else {
        MusitFuture.successful(Seq.empty[RelatedEvents])
      }

    futureSubEvents.musitFutureFlatMap(subEvents => createEventInMemory(baseEventDto, subEvents))
  }

  /**
   * TODO: Document me!!!
   */
  def getEvent(id: Long, recursive: Boolean): MusitFuture[MusitEvent] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture(ErrorHelper.badRequest(s"Event with id: $id not found"))

    maybeBaseEventDto.musitFutureFlatMap { baseEventDto =>
      getEvent(baseEventDto, recursive)
    }
  }

  /**
   * Gets the sub-events from the event with id=parentId from the database.
   * Embedded in the proper relations (RelatedEvents-objects)
   *
   * TODO: Document me!!!
   */
  // scalastyle:off
  def getSubEvents(parentId: Long, recursive: Boolean): MusitFuture[Seq[RelatedEvents]] = {

    def getEventsFromEventDtos(eventDtos: MusitFuture[Seq[BaseEventDto]]): MusitFuture[Seq[MusitEvent]] = {
      eventDtos.musitFutureFlatMap {
        subEventDtos: Seq[BaseEventDto] =>
          MusitFuture.traverse(subEventDtos) { subEventDto =>
            getEvent(subEventDto, recursive)
          }
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
      getEventsFromEventDtos(futureSubEventDtos).musitFutureMap { events =>
        RelatedEvents(EventRelations.relation_parts, events)
      }
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
  } // scalastyle:on

}
