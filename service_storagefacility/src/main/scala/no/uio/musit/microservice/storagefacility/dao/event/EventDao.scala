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
import no.uio.musit.microservice.storagefacility.dao.event.EventRelationTypes.PartialEventRelation
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event.dto._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, _}
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.SequenceAction

import scala.concurrent.Future

class EventDao @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  val eventLinkDao: EventRelationDao,
  val obsFromToDao: ObservationFromToDao,
  val obsPestDao: ObservationPestDao
) extends SharedEventTables with ColumnTypeMappers {

  import driver.api._

  private val eventBaseTable = TableQuery[EventBaseTable]

  /**
   *
   * @param eventBaseDto
   * @return
   */
  def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[Long] = {
    eventBaseTable returning eventBaseTable.map(_.id) += eventBaseDto
  }

  /**
   * Helper to build up the correct insert action depending on Dto type.
   */
  def buildInsertAction(event: Dto, parentId: Option[Long]): DBIO[Long] = {
    event match {
      case simple: BaseEventDto =>
        insertBaseAction(simple.copy(partOf = parentId))

      case complex: ExtendedDto =>
        val base = complex.baseEventDto.copy(partOf = parentId)
        insertBaseAction(base).flatMap { eventId =>
          val extendedAction: DBIO[Int] = complex.extension match {
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
          extendedAction.map(_ => eventId)
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
    partialRelation: Option[PartialEventRelation]
  ): DBIO[Long] = {

    // We want partOfParent to be Some(parentId) if event has a parent and that
    // parent is in the parts-relation to us.
    // TODO: The following two lines of code is very confusing, what is it supposed to do?
    val isPartsRelation = partialRelation.exists(_.relation == EventRelations.relation_parts)
    val partOfParent = partialRelation.filter(_ => isPartsRelation).map(_.idFrom)

    val insertBase = buildInsertAction(event, partOfParent)

    val insertWithChildren =
      for {
        theEventId <- insertBase
        _ <- insertChildrenAction(theEventId, event.relatedSubEvents)
        _ <- {
          partialRelation.filterNot(_ => isPartsRelation).map { pel =>
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
          Some(PartialEventRelation(parentEventId, relatedEvents.relation))
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
    val action = eventBaseTable.filter(_.id === id).result.headOption
    db.run(action)
  }

  private def initCompleteDto(
    baseEventDto: BaseEventDto,
    relatedSubEvents: Seq[RelatedEvents]
  ): MusitFuture[Dto] = {
    // The ID field will never be None or null in this context
    val baseWithSubs = baseEventDto.copy(relatedSubEvents = relatedSubEvents)

    EventTypeRegistry.unsafeSubFromId[SubEventType](baseWithSubs.eventTypeId) match {
      case ctrlSub: CtrlSubEventType =>
        // Standard control sub-events are plain BaseEventDto's
        MusitFuture.successful(baseWithSubs)

      case obsSub: ObsSubEventType =>
        obsSub match {
          case ObsHumidityType | ObsTemperatureType | ObsHypoxicAirType =>
            // We are dealing with an ObservationFromToDto now, go fetch
            // additional data required for building up the result.
            obsFromToDao.getObservationFromTo(baseWithSubs.id.get).map { mft =>
              mft.map { ft =>
                ExtendedDto(baseWithSubs, ft)
              }.toRight(
                MusitError(404, "Could not find ObservationFromTo data")
              )
            }

          case ObsPestType =>
            // We've got an ObservationPestDto on our hands.
            obsPestDao.getObservation(baseWithSubs.id.get).map { maybePest =>
              maybePest.map { pest =>
                ExtendedDto(baseWithSubs, pest)
              }.toRight(
                MusitError(404, "Could not find ObservationPest data")
              )
            }

          case others =>
            // Any other Observation sub-events are BaseEventDto's
            MusitFuture.successful(baseWithSubs)
        }
    }

  }

  private def getEvent(
    baseEventDto: BaseEventDto,
    recursive: Boolean
  ): MusitFuture[Dto] = {
    val futureSubEvents =
      if (recursive) {
        // ID can never be None or null in this context, so it's safe to .get
        getSubEvents(baseEventDto.id.get, recursive)
      } else {
        MusitFuture.successful(Seq.empty[RelatedEvents])
      }

    futureSubEvents.musitFutureFlatMap { subEvents =>
      initCompleteDto(baseEventDto, subEvents)
    }
  }

  /**
   * TODO: Document me!!!
   *
   * @param id
   * @param recursive Boolean indicating if children should be returned,
   *                  defaults to true.
   * @return The Dto containing the event data.
   */
  def getEvent(id: Long, recursive: Boolean = true): MusitFuture[Dto] = {

    val maybeBaseEventDto = getBaseEvent(id).toMusitFuture {
      ErrorHelper.badRequest(s"Event with id: $id not found")
    }

    maybeBaseEventDto.musitFutureFlatMap { baseEventDto =>
      getEvent(baseEventDto, recursive)
    }
  }

  /**
   * Get the children (parts) for the event with the given parentId.
   */
  private def getSubEventDtos(parentId: Long): Future[Seq[BaseEventDto]] = {
    val action = eventBaseTable.filter(_.partOf === parentId).result
    db.run(action)
  }

  /**
   * TODO: Document me!
   */
  private def getEventsFromDtos(
    eventDtos: MusitFuture[Seq[BaseEventDto]],
    recursive: Boolean
  ): MusitFuture[Seq[Dto]] = {
    eventDtos.musitFutureFlatMap {
      subEventDtos: Seq[BaseEventDto] =>
        MusitFuture.traverse(subEventDtos) { subEventDto =>
          getEvent(subEventDto, recursive)
        }
    }
  }

  /**
   * Fetch the parts/partOf relation
   */
  private def getPartEvents(
    parentId: Long,
    recursive: Boolean
  ): MusitFuture[RelatedEvents] = {
    val futureSubEventDtos = getSubEventDtos(parentId).toMusitFuture
    //Create a parts-relation of the these subEvents
    getEventsFromDtos(futureSubEventDtos, recursive).musitFutureMap { events =>
      RelatedEvents(EventRelations.relation_parts, events)
    }
  }

  //The "other" relations, those stored in the event_relation_event table
  private def getOtherRelatedEvents(
    parentId: Long,
    recursive: Boolean
  ): MusitFuture[Seq[RelatedEvents]] = {

    def transform(group: Seq[(Int, BaseEventDto)]): MusitFuture[Seq[Dto]] = {
      val groupedEvents = group.map(_._2).map(dto => getEvent(dto, recursive))
      //Collapse the inner musitresults...
      MusitFuture.traverse[MusitFuture[Dto], Dto](groupedEvents)(identity)
    }

    val futureRelatedDtos = eventLinkDao.getRelatedEventDtos(parentId).toMusitFuture

    futureRelatedDtos.musitFutureFlatMap { relatedDtos =>
      val groups = relatedDtos.groupBy(_._1).map {
        case (relationId, related) =>
          (relationId, transform(related))
      }.toSeq

      val result = MusitFuture.traverse(groups) {
        case (relId, futureEvents) =>
          futureEvents.musitFutureMap { events =>
            // FIXME: Unsafe operation
            RelatedEvents(EventRelations.getByIdOrFail(relId), events)
          }
      }
      result
    }
  }

  /**
   * Gets the sub-events from the event with id=parentId from the database.
   * Embedded in the proper relations (RelatedEvents-objects)
   *
   * TODO: Document me!!!
   */
  def getSubEvents(
    parentId: Long,
    recursive: Boolean
  ): MusitFuture[Seq[RelatedEvents]] = {
    val futurePartsEvents = getPartEvents(parentId, recursive)
    val futureOtherRelatedEvents = getOtherRelatedEvents(parentId, recursive)

    //Now we simply need to join the parts-events and the other related events
    futurePartsEvents.musitFutureFlatMap { partEvents =>
      futureOtherRelatedEvents.musitFutureMap { extraRelatedEvents =>
        if (partEvents.events.isEmpty) extraRelatedEvents
        else partEvents +: extraRelatedEvents
      }
    }
  }

}
