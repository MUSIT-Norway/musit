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

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.dao.ColumnTypeMappers
import no.uio.musit.microservice.storagefacility.dao.event.EventRelationTypes.PartialEventRelation
import no.uio.musit.microservice.storagefacility.domain.MusitResults._
import no.uio.musit.microservice.storagefacility.domain.event.{ EventType, EventTypeId, EventTypeRegistry, MusitEvent }
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event.dto._
import no.uio.musit.microservice.storagefacility.domain.storage.StorageNodeId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.SequenceAction

import scala.concurrent.Future
import scala.reflect.ClassTag

/*
  TODO:
    The code here is a very big source for potential bugs. There's a lot of
    unnecessary complexity due to its generic nature. Refactoring is a must!

    It should ideally be split into DAOs that handle each type in full:
    - one for Control
    - one for Observation
    - one for EnvRequirement
    - etc...

    This partitioning would have been easy to spot if the impl had followed  a
    top-down approach. Where the first "implementation" of the persistence layer
    would've had to be a "repository" trait for each of the main types.
 */

/**
 * TODO: Document me!!!
 */
@Singleton
class EventDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val eventRelationDao: EventRelationDao,
    val obsFromToDao: ObservationFromToDao,
    val obsPestDao: ObservationPestDao,
    val envReqDao: EnvRequirementDao,
    val evtActorsDao: EventActorsDao,
    val evtObjectsDao: EventObjectsDao,
    val evtPlacesDao: EventPlacesDao,
    val evtPlacesAsObjDao: EventPlacesAsObjectsDao
) extends SharedEventTables with ColumnTypeMappers {

  import driver.api._

  private val logger = Logger(classOf[EventDao])

  private val eventBaseTable = TableQuery[EventBaseTable]

  /**
   * Assembles an action for inserting the base denominator of an event into the
   * base event table.
   *
   * @param eventBaseDto The BaseEventDto to insert
   * @return DBIO[Long]
   */
  private def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[Long] = {
    eventBaseTable returning eventBaseTable.map(_.id) += eventBaseDto
  }

  /**
   * Helper to build up the correct insert action depending on Dto type.
   */
  private def buildInsertAction(event: Dto, parentId: Option[Long]): DBIO[Long] = {
    event match {
      case simple: BaseEventDto =>
        insertBaseAction(simple.copy(partOf = parentId))

      case complex: ExtendedDto =>
        val base = complex.baseEventDto.copy(partOf = parentId)
        insertBaseAction(base).flatMap { eventId =>
          val extendedAction: DBIO[Int] = complex.extension match {
            case ext: ObservationFromToDto =>
              obsFromToDao.insertAction(ext.copy(id = Option(eventId)))

            case ext: ObservationPestDto =>
              obsPestDao.insertAction(eventId, ext)

            case ext: EnvRequirementDto =>
              envReqDao.insertAction(ext.copy(id = Option(eventId)))
          }
          extendedAction.map(_ => eventId)
        }
    }
  }

  private[this] def insertRelatedType[A: ClassTag](
    eventId: Long,
    relType: Seq[A]
  )(insert: (Long, Seq[A]) => DBIO[Option[Int]]): DBIO[Option[Int]] = {
    if (relType.nonEmpty) insert(eventId, relType)
    else DBIO.successful[Option[Int]](None)
  }

  private[this] def insertRelatedActors(
    eventId: Long,
    actors: Seq[EventRoleActor]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, actors)(evtActorsDao.insertActors)

  private[this] def insertRelatedObjects(
    eventId: Long,
    objects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, objects)(evtObjectsDao.insertObjects)

  private[this] def insertRelatedPlaces(
    eventId: Long,
    places: Seq[EventRolePlace]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, places)(evtPlacesDao.insertPlaces)

  private[this] def insertRelatedObjectsAsPlaces(
    eventId: Long,
    objPlaces: Seq[EventRoleObject]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, objPlaces)(evtPlacesAsObjDao.insertObjects)

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
    // TODO: The following two lines of code are very confusing, what is it supposed to do?
    val isPartsRelation = partialRelation.exists(_.relation == EventRelations.relation_parts)
    val partOfParent = partialRelation.filter(_ => isPartsRelation).map(_.idFrom)

    val insertBase = buildInsertAction(event, partOfParent)

    val insertWithChildren =
      for {
        // Execute the insert of the base event
        theEventId <- insertBase
        // Insert the children
        _ <- insertChildrenAction(theEventId, event.relatedSubEvents)
        // Insert the event relations
        _ <- {
          partialRelation.filterNot(_ => isPartsRelation).map { pel =>
            eventRelationDao.insertRelationAction(pel.toFullLink(theEventId))
          }.getOrElse(DBIO.successful[Int](0))
        }
        // Insert any related actor relations
        _ <- insertRelatedActors(theEventId, event.relatedActors)
        // Insert any related objects relations
        _ <- {
          if (MoveObjectType.id == event.eventTypeId) {
            insertRelatedObjects(theEventId, event.relatedObjects)
          } else {
            insertRelatedObjectsAsPlaces(theEventId, event.relatedObjects)
          }
        }
        // Insert any related place relations
        _ <- insertRelatedPlaces(theEventId, event.relatedPlaces)
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
  private def insertChildrenAction(parentEventId: Long, children: Seq[RelatedEvents]) = {
    val actions = children.map { relatedEvents =>
      val relActions = relatedEvents.events.map { subEvent =>
        insertEventAction(
          subEvent,
          Some(PartialEventRelation(parentEventId, relatedEvents.relation))
        )
      } // TODO: also handle other relations than parts
      SequenceAction(relActions.toIndexedSeq)
    }

    SequenceAction(actions.toIndexedSeq)
  }

  /**
   * TODO: Document me!!!
   */
  def getBaseEvent(
    id: Long,
    eventTypeId: Option[EventTypeId] = None
  ): Future[Option[BaseEventDto]] = {

    val action = {
      eventTypeId.map { etid =>
        eventBaseTable.filter(_.eventTypeId === etid).filter(_.id === id)
      }.getOrElse {
        eventBaseTable.filter(_.id === id)
      }.result.headOption
    }
    val futureBaseEvent = db.run(action)

    for {
      maybeBase <- futureBaseEvent
      actors <- evtActorsDao.getRelatedActors(id)
      objects <- {
        maybeBase.map { dto =>
          // Only cases where the event is a MoveObject event is the
          // related object in the evtObjectsDao
          if (MoveObjectType.id == dto.eventTypeId) {
            evtObjectsDao.getRelatedObjects(id)
          } else {
            evtPlacesAsObjDao.getRelatedObjects(id)
          }
        }.getOrElse {
          Future.successful(Seq.empty)
        }
      }
      places <- evtPlacesDao.getRelatedPlaces(id)
    } yield {
      maybeBase.map {
        _.copy(
          relatedActors = actors,
          relatedObjects = objects,
          relatedPlaces = places
        )
      }
    }
  }

  /**
   * TODO: Document me!
   */
  private def enrichDto(dto: Dto): Future[Dto] = {

    val base: BaseEventDto = dto match {
      case b: BaseEventDto => b
      case e: ExtendedDto => e.baseEventDto
    }

    val tpe = EventTypeRegistry.unsafeSubFromId[SubEventType](base.eventTypeId)

    tpe match {
      case ctrlSub: CtrlSubEventType =>
        logger.debug(s"found CtrlSubEventType ${ctrlSub.entryName}")
        Future.successful(base)

      case obsSub: ObsSubEventType =>
        logger.debug(s"found ObsSubEventType ${obsSub.entryName}")
        obsSub match {
          case ObsHumidityType | ObsTemperatureType | ObsHypoxicAirType =>
            // We are dealing with an ObservationFromToDto now, go fetch
            // additional data required for building up the result.
            obsFromToDao.getObservationFromTo(base.id.get).map { mft =>
              mft.map { ft =>
                ExtendedDto(base, ft)
              }.getOrElse {
                logger.warn("Could not find ObservationFromTo data using BaseEvent data")
                base
              }
            }

          case ObsPestType =>
            // We've got an ObservationPestDto on our hands.
            obsPestDao.getObservation(base.id.get).map { maybePest =>
              maybePest.map { pest =>
                ExtendedDto(base, pest)
              }.getOrElse {
                // FIXME: MusitError must be changed
                logger.warn("Could not find ObservationPest data using BaseEvent data")
                base
              }
            }

          case others =>
            // Any other Observation sub-events are BaseEventDto's
            Future.successful(base)
        }
    }
  }

  // FIXME: All of this code would be unnecessary with stronger typing of DTO's
  private def initCompleteDto(
    baseEventDto: BaseEventDto,
    relatedSubEvents: Seq[RelatedEvents]
  ): Future[MusitResult[Option[Dto]]] = {
    val related = relatedSubEvents.map { subs =>
      val futureSubEvents = Future.traverse(subs.events)(re => enrichDto(re))
      futureSubEvents.map(se => subs.copy(events = se))
    }

    Future.sequence(related).map { rel =>
      MusitSuccess(Option(baseEventDto.copy(relatedSubEvents = rel)))
    }
  }

  private def getFullEvent(
    baseEventDto: BaseEventDto,
    recursive: Boolean
  ): Future[MusitResult[Option[Dto]]] = {

    EventTypeRegistry.unsafeFromId(baseEventDto.eventTypeId) match {
      case EnvRequirementEventType =>
        envReqDao.getEnvRequirement(baseEventDto.id.get).map { mer =>
          MusitSuccess[Option[Dto]](Option(
            mer.map(er => ExtendedDto(baseEventDto, er)).getOrElse(baseEventDto)
          ))
        }

      case MoveObjectType | MoveNodeType =>
        Future.successful(MusitSuccess(Option(baseEventDto)))

      case _ =>
        val futureSubEvents =
          if (recursive) {
            // ID can never be None or null in this context, so it's safe to .get
            getSubEvents(baseEventDto.id.get, recursive)
          } else {
            Future.successful(Seq.empty[RelatedEvents])
          }

        futureSubEvents.flatMap { subEvents =>
          initCompleteDto(baseEventDto, subEvents)
        }
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
  def getEvent(
    id: Long,
    recursive: Boolean = true,
    eventTypeId: Option[EventTypeId] = None
  ): Future[MusitResult[Option[Dto]]] = {
    getBaseEvent(id, eventTypeId).flatMap { maybeDto =>
      maybeDto.map { base =>
        logger.debug(s"Found base event $base. Going to fetch full event")
        getFullEvent(base, recursive)
      }.getOrElse {
        logger.debug(s"No event data found for id $id.")
        Future.successful(MusitSuccess[Option[Dto]](None))
      }
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
    eventDtos: Future[Seq[BaseEventDto]],
    recursive: Boolean
  ): Future[Seq[Dto]] = {
    eventDtos.flatMap { subEventDtos =>
      Future.traverse(subEventDtos) { subEventDto =>
        getFullEvent(subEventDto, recursive)
      }.map { results =>
        // We remove the failed futures and return only the successful ones.
        // NOTE: We're discarding useful information about failure here.
        results.filter(_.isSuccess).map {
          case MusitSuccess(maybeSuccess) =>
            maybeSuccess

          case notPossible =>
            throw new IllegalStateException("Encountered impossible state") // scalastyle:ignore

        }.filter(_.isDefined).map(_.get)
      }
    }
  }

  /**
   * Fetch the parts/partOf relation
   */
  private def getPartEvents(
    parentId: Long,
    recursive: Boolean
  ): Future[RelatedEvents] = {
    val futureSubEventDtos = getSubEventDtos(parentId)
    //Create a parts-relation of the these subEvents
    getEventsFromDtos(futureSubEventDtos, recursive).map { events =>
      RelatedEvents(EventRelations.relation_parts, events)
    }
  }

  //The "other" relations, those stored in the event_relation_event table
  private def getOtherRelatedEvents(
    parentId: Long,
    recursive: Boolean
  ): Future[Seq[RelatedEvents]] = {

    def transform(group: Seq[(Int, BaseEventDto)]): Future[Seq[Dto]] = {
      val grpdEvents = group.map(_._2).map(dto => getFullEvent(dto, recursive))
      //Collapse the inner musitresults...
      Future.traverse(grpdEvents)(identity).map { results =>
        // NOTE: We're discarding useful information about failure here.
        results.filter(_.isSuccess).map {
          case MusitSuccess(maybeSuccess) =>
            maybeSuccess

          case notPossible =>
            throw new IllegalStateException("Encountered impossible state") // scalastyle:ignore

        }.filter(_.isDefined).map(_.get)
      }
    }

    val futureRelatedDtos = eventRelationDao.getRelatedEvents(parentId)

    futureRelatedDtos.flatMap { relatedDtos =>
      val groups = relatedDtos.groupBy(_._1).map {
        case (relationId, related) =>
          (relationId, transform(related))
      }.toSeq

      Future.traverse(groups) {
        case (relId, futureEvents) =>
          futureEvents.map { events =>
            RelatedEvents(EventRelations.unsafeGetById(relId), events)
          }
      }
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
  ): Future[Seq[RelatedEvents]] = {
    val futurePartsEvents = getPartEvents(parentId, recursive)
    val futureOtherRelatedEvents = getOtherRelatedEvents(parentId, recursive)

    //Now we simply need to join the parts-events and the other related events
    futurePartsEvents.flatMap { partEvents =>
      futureOtherRelatedEvents.map { extraRelatedEvents =>
        if (partEvents.events.isEmpty) extraRelatedEvents
        else partEvents +: extraRelatedEvents
      }
    }
  }

  // TODO: Should probably use a Limit type to support paging.
  /**
   * This method is quite sub-optimally implemented. It will first trigger a
   * query against the "EVENT_ROLE_PLACE_AS_OBJECT" to get all the relevant
   * eventIds. Then it will iterate that list and fire queries to fetch each
   * eventId individually. This could be quite costly in the long run.
   */
  def getEventsForNode(
    nodeId: StorageNodeId,
    eventType: TopLevelEvent
  ): Future[Seq[Dto]] = {
    // First fetch the eventIds from the place as object relation table.
    val futureEventIds = evtPlacesAsObjDao.getEventsForObjects(nodeId.toInt).map { objs =>
      objs.map(_.eventId).filter(_.isDefined).map(_.get)
    }
    // Iterate the list of IDs and fetch the related event.
    futureEventIds.flatMap { ids =>
      Future.traverse(ids) { eventId =>
        getEvent(
          id = eventId,
          eventTypeId = Some(eventType.id)
        )
      }.map { results =>
        results.filter(_.isSuccess).map {
          case MusitSuccess(maybeSuccess) =>
            maybeSuccess

          case notPossible =>
            throw new IllegalStateException("Encountered impossible state") // scalastyle:ignore

        }.filter(_.isDefined).map(_.get)
      }
    }
  }

}
