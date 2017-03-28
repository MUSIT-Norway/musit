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

package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.ObsSubEvents._
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.EventTypeRegistry._
import models.storage.event.dto.DtoConverters.MoveConverters
import models.storage.event.dto._
import models.storage.event.old.move.MoveObject
import models.storage.event.{EventTypeId, EventTypeRegistry, MusitEvent_Old}
import no.uio.musit.MusitResults._
import no.uio.musit.models.{EventId, MuseumId, ObjectId, StorageNodeDatabaseId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.ColumnTypeMappers
import repositories.storage.old_dao.event.EventRelationTypes.PartialEventRelation
import repositories.storage.old_dao.{EventTables, LocalObjectDao}
import slick.dbio.Effect.All
import slick.dbio.SequenceAction

import scala.collection.immutable.IndexedSeq
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

    This correct abstraction would have been easy to spot if the impl had
    followed  a top-down approach. Where the first "implementation" of the
    persistence layer would've had to be a "repository" trait for each of the
    main types.
 */

/**
 * TODO: Document me!!!
 */
@Singleton
class EventDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider,
    relationDao: EventRelationDao,
    obsFromToDao: ObservationFromToDao,
    obsPestDao: ObservationPestDao,
    envReqDao: EnvRequirementDao,
    actorsDao: EventActorsDao,
    objectsDao: EventObjectsDao,
    placesDao: EventPlacesDao,
    placesAsObjDao: EventPlacesAsObjectsDao,
    localObjectDao: LocalObjectDao
) extends EventTables
    with ColumnTypeMappers {

  import profile.api._

  private val logger = Logger(classOf[EventDao])

  /**
   * Assembles an action for inserting the base denominator of an event into the
   * base event table.
   *
   * @param eventBaseDto The BaseEventDto to insert
   * @return DBIO[Long]
   */
  private def insertBaseAction(eventBaseDto: BaseEventDto): DBIO[EventId] = {
    eventBaseTable returning eventBaseTable.map(_.id) += eventBaseDto
  }

  /**
   * Helper to build up the correct insert action depending on Dto type.
   */
  private def buildInsertAction(
      event: EventDto,
      parentId: Option[EventId]
  ): DBIO[EventId] = {
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
      eventId: EventId,
      relType: Seq[A]
  )(insert: (EventId, Seq[A]) => DBIO[Option[Int]]): DBIO[Option[Int]] = {
    if (relType.nonEmpty) insert(eventId, relType)
    else DBIO.successful[Option[Int]](None)
  }

  private[this] def insertRelatedActors(
      eventId: EventId,
      actors: Seq[EventRoleActor]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, actors)(actorsDao.insertActors)

  private[this] def insertRelatedObjects(
      eventId: EventId,
      objects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, objects)(objectsDao.insertObjects)

  private[this] def insertRelatedPlaces(
      eventId: EventId,
      places: Seq[EventRolePlace]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, places)(placesDao.insertPlaces)

  private[this] def insertRelatedObjectsAsPlaces(
      mid: MuseumId,
      eventId: EventId,
      objPlaces: Seq[EventRoleObject]
  ): DBIO[Option[Int]] =
    insertRelatedType(eventId, objPlaces)(placesAsObjDao.insertObjects)

  /**
   * Creates an action to insert the event and potentially all related
   * sub-events. PartialEventLink is used if the event is inserted as a
   * subElement of a parent element. The id of the parent element is then in
   * the partialEventLink.
   *
   * WARNING: This function is recursive with the possibility of triggering an
   * infinite loop. Due to the event DTO model being way to generic and its
   * abstraction incorrectly (for storage facility) represented as recursive,
   * there is a possibility for building an structure that references itself.
   * This will cause an infinite loop and eventually a stack overflow.
   */
  private[this] def insertEventAction(
      mid: MuseumId,
      event: EventDto,
      partialRelation: Option[PartialEventRelation]
  ): DBIO[EventId] = {

    // We want partOfParent to be Some(parentId) if event has a parent and that
    // parent is in the parts-relation to us.
    val isPartsRelation = partialRelation.exists { per =>
      per.relation == EventRelations.PartsOfRelation
    }
    val partOfParent = partialRelation.filter(_ => isPartsRelation).map(_.idFrom)
    val insertBase   = buildInsertAction(event, partOfParent)

    for {
      // Execute the insert of the base event
      theEventId <- insertBase
      // Insert the children
      _ <- insertChildrenAction(mid, theEventId, event.relatedSubEvents)
      // Insert the event relations
      _ <- {
        partialRelation
          .filterNot(_ => isPartsRelation)
          .map { pel =>
            relationDao.insertRelationAction(pel.toFullLink(theEventId))
          }
          .getOrElse(DBIO.successful[Int](0))
      }
      // Insert any related actor relations
      _ <- insertRelatedActors(theEventId, event.relatedActors)
      // Insert any related objects relations
      _ <- {
        if (MoveObjectType.id == event.eventTypeId) {
          localObjectDao
            .storeLatestMove(mid, theEventId, event)
            .andThen(
              insertRelatedObjects(theEventId, event.relatedObjects)
            )
        } else {
          insertRelatedObjectsAsPlaces(mid, theEventId, event.relatedObjects)
        }
      }
      // Insert any related place relations
      _ <- insertRelatedPlaces(theEventId, event.relatedPlaces)
    } yield theEventId
  }

  /**
   * TODO: Document me!!!
   */
  def insertEvent(mid: MuseumId, event: EventDto): Future[EventId] = {
    val action = insertEventAction(mid, event, None)
    db.run(action.transactionally)
  }

  /**
   * Allows batch insertion of multiple events wrapped in 1 transaction.
   *
   * @param mid    MuseumId
   * @param events collection of EventDto instances to insert
   * @return Eventually returns a list of EventIds for the inserted events.
   */
  def insertEvents(mid: MuseumId, events: Seq[EventDto]): Future[Seq[EventId]] = {
    val actions = DBIO.sequence(events.map(e => insertEventAction(mid, e, None)))
    db.run(actions.transactionally)
  }

  /**
   * TODO: Document me!!!
   */
  private def insertChildrenAction(
      mid: MuseumId,
      parentEventId: EventId,
      children: Seq[RelatedEvents]
  ): SequenceAction[IndexedSeq[EventId], IndexedSeq[IndexedSeq[EventId]], All] = {
    val actions = children.map { relatedEvents =>
      val relActions = relatedEvents.events.map { subEvent =>
        insertEventAction(
          mid,
          subEvent,
          Some(PartialEventRelation(parentEventId, relatedEvents.relation))
        )
      }
      SequenceAction(relActions.toIndexedSeq)
    }

    SequenceAction(actions.toIndexedSeq)
  }

  /**
   * TODO: Document me!!!
   */
  def getBaseEvent(
      mid: MuseumId,
      id: EventId,
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
      actors    <- actorsDao.getRelatedActors(id)
      objects <- {
        maybeBase.map { dto =>
          // Only cases where the event is a MoveObject event is the
          // related object in the evtObjectsDao
          if (MoveObjectType.id == dto.eventTypeId) {
            objectsDao.getRelatedObjects(id)
          } else {
            placesAsObjDao.getRelatedObjects(mid, id)
          }
        }.getOrElse {
          Future.successful(Seq.empty)
        }
      }
      places <- placesDao.getRelatedPlaces(mid, id)
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
  private def enrichDto(dto: EventDto): Future[EventDto] = {

    val base: BaseEventDto = dto match {
      case b: BaseEventDto => b
      case e: ExtendedDto  => e.baseEventDto
    }

    // TODO: Change to use safe operations for each event type
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

  private def initCompleteDto(
      baseEventDto: BaseEventDto,
      relatedSubEvents: Seq[RelatedEvents]
  ): Future[MusitResult[Option[EventDto]]] = {
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
  ): Future[MusitResult[Option[EventDto]]] = {

    EventTypeRegistry.unsafeFromId(baseEventDto.eventTypeId) match {
      case EnvRequirementEventType =>
        envReqDao.getEnvRequirement(baseEventDto.id.get).map { mer =>
          MusitSuccess(
            Option(
              mer.map(er => ExtendedDto(baseEventDto, er)).getOrElse(baseEventDto)
            )
          )
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
   * @param eventTypeId
   * @param recursive Boolean indicating if children should be returned,
   *                  defaults to true.
   * @return The Dto containing the event data.
   */
  def getEvent(
      mid: MuseumId,
      id: EventId,
      eventTypeId: Option[EventTypeId] = None,
      recursive: Boolean = true
  ): Future[MusitResult[Option[EventDto]]] = {
    getBaseEvent(mid, id, eventTypeId).flatMap { maybeDto =>
      maybeDto.map { base =>
        logger.debug(s"Found base event $base. Going to fetch full event")
        getFullEvent(base, recursive)
      }.getOrElse {
        logger.debug(s"No event data found for id $id.")
        Future.successful(MusitSuccess(None))
      }
    }
  }

  /**
   * Get the latest event for a given nodeId.
   *
   * @param id
   * @param eventTypeId
   */
  def latestByNodeId(
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      eventTypeId: EventTypeId
  ): Future[MusitResult[Option[EventDto]]] = {
    for {
      maybeEventId <- placesAsObjDao.latestEventIdFor(mid, id, eventTypeId)
      evt <- {
        logger.debug(s"Latest eventId for node $id is $maybeEventId")
        maybeEventId.map { erp =>
          getEvent(
            mid,
            // We can use Option.get here because it must have a value.
            id = maybeEventId.get,
            eventTypeId = Some(eventTypeId)
          )
        }.getOrElse(Future.successful(MusitSuccess(None)))
      }
    } yield {
      logger.debug(s"Returning result $evt")
      evt
    }
  }

  /**
   * Get the children (parts) for the event with the given parentId.
   */
  private def getSubEventDtos(parentId: EventId): Future[Seq[BaseEventDto]] = {
    val action = eventBaseTable.filter(_.partOf === parentId).result
    db.run(action)
  }

  /**
   * TODO: Document me!
   */
  private def getEventsFromDtos(
      eventDtos: Future[Seq[BaseEventDto]],
      recursive: Boolean
  ): Future[Seq[EventDto]] = {
    eventDtos.flatMap { subEventDtos =>
      Future
        .traverse(subEventDtos) { subEventDto =>
          getFullEvent(subEventDto, recursive)
        }
        .map { results =>
          // We remove the failed futures and return only the successful ones.
          // NOTE: We're discarding useful information about failure here.
          results
            .filter(_.isSuccess)
            .map {
              case MusitSuccess(maybeSuccess) =>
                maybeSuccess

              case notPossible =>
                throw new IllegalStateException("Encountered impossible state")

            }
            .filter(_.isDefined)
            .map(_.get)
        }
    }
  }

  /**
   * Fetch the parts/partOf relation
   */
  private def getPartEvents(
      parentId: EventId,
      recursive: Boolean
  ): Future[RelatedEvents] = {
    val futureSubEventDtos = getSubEventDtos(parentId)
    //Create a parts-relation of the these subEvents
    getEventsFromDtos(futureSubEventDtos, recursive).map { events =>
      RelatedEvents(EventRelations.PartsOfRelation, events)
    }
  }

  /**
   * The "other" relations, those stored in the event_relation_event table
   */
  private def getOtherRelatedEvents(
      parentId: EventId,
      recursive: Boolean
  ): Future[Seq[RelatedEvents]] = {

    def transform(group: Seq[(Int, BaseEventDto)]): Future[Seq[EventDto]] = {
      val grpdEvents = group.map(_._2).map(dto => getFullEvent(dto, recursive))
      //Collapse the inner musitresults...
      Future.traverse(grpdEvents)(identity).map { results =>
        // NOTE: We're discarding useful information about failure here.
        results
          .filter(_.isSuccess)
          .map {
            case MusitSuccess(maybeSuccess) =>
              maybeSuccess

            case notPossible =>
              throw new IllegalStateException("Encountered impossible state")

          }
          .filter(_.isDefined)
          .map(_.get)
      }
    }

    val futureRelatedDtos = relationDao.getRelatedEvents(parentId)

    futureRelatedDtos.flatMap { relatedDtos =>
      val groups = relatedDtos
        .groupBy(_._1)
        .map {
          case (relationId, related) =>
            (relationId, transform(related))
        }
        .toSeq

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
      parentId: EventId,
      recursive: Boolean
  ): Future[Seq[RelatedEvents]] = {
    val futurePartsEvents        = getPartEvents(parentId, recursive)
    val futureOtherRelatedEvents = getOtherRelatedEvents(parentId, recursive)

    //Now we simply need to join the parts-events and the other related events
    futurePartsEvents.flatMap { partEvents =>
      futureOtherRelatedEvents.map { extraRelatedEvents =>
        if (partEvents.events.isEmpty) extraRelatedEvents
        else partEvents +: extraRelatedEvents
      }
    }
  }

  /**
   * Function used for migration of data from old event table(s) to new.
   */
  def getAllEvents[A <: TopLevelEvent, Res <: MusitEvent_Old](
      mid: MuseumId,
      eventType: A
  )(success: EventDto => Res): Future[Seq[Res]] = {
    val getIds = eventBaseTable.filter(_.eventTypeId === eventType.id).map(_.id).result

    db.run(getIds).flatMap { ids =>
      Future.traverse(ids)(eid => getEvent(mid, eid, Some(eventType.id))).map { res =>
        res
          .filter(_.isSuccess)
          .map {
            case MusitSuccess(ms) =>
              ms.map(dto => success(dto))

            case err: MusitError =>
              logger.error(err.message)
              throw new IllegalStateException("Impossible state") // scalastyle:ignore
          }
          .filter(_.isDefined)
          .map(_.get)
      }
    }
  }

  /**
   * This method is quite sub-optimally implemented. It will first trigger a
   * query against the "EVENT_ROLE_PLACE_AS_OBJECT" to get all the relevant
   * eventIds. Then it will iterate that list and fire queries to fetch each
   * eventId individually. This could be quite costly in the long run.
   *
   * The method encapsulates commonly used logic to fetch specific event types
   * for a given StorageNodeId.
   */
  private def eventsForNode[EType <: TopLevelEvent, Res](
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId,
      eventType: EType,
      limit: Option[Int] = None
  )(success: EventDto => Res): Future[Seq[Res]] = {
    val futureEventIds = placesAsObjDao.latestEventIdsForNode(
      mid,
      nodeId = nodeId,
      eventTypeId = eventType.id,
      limit = limit
    )

    futureEventIds.flatMap { ids =>
      Future.traverse(ids)(eId => getEvent(mid, eId, Some(eventType.id))).map { res =>
        res
          .filter(_.isSuccess)
          .map {
            case MusitSuccess(maybeSuccess) =>
              maybeSuccess.map(dto => success(dto))

            case impossible =>
              throw new IllegalStateException("Encountered impossible state")

          }
          .filter(_.isDefined)
          .map(_.get)
      }
    }
  }

  /**
   * Fetch events of a given TopLevelEvent type for the given StorageNodeId
   *
   * @param mid       MuseumId
   * @param id        StorageNodeId to get events for
   * @param eventType TopLevelEvent type to fetch
   * @tparam A type argument specifying the type of TopLevelEvent to fetch
   * @return A Future of a collection of EventDtos
   */
  def getEventsForNode[A <: TopLevelEvent](
      mid: MuseumId,
      id: StorageNodeDatabaseId,
      eventType: A
  ): Future[Seq[EventDto]] = eventsForNode(mid, id, eventType)(dto => dto)

  /**
   *
   * @param mid
   * @param objectId
   * @param eventType
   * @param limit
   * @param success
   * @tparam EType
   * @tparam Res
   * @return
   */
  private def eventsForObject[EType <: TopLevelEvent, Res](
      mid: MuseumId,
      objectId: ObjectId,
      eventType: EType,
      limit: Option[Int] = None
  )(success: EventDto => Res): Future[Seq[Res]] = {
    val futureEventIds = objectsDao.latestEventIdsForObject(
      objectId = objectId,
      eventTypeId = eventType.id,
      limit = limit
    )

    futureEventIds.flatMap { ids =>
      Future.traverse(ids)(eId => getEvent(mid, eId, Some(eventType.id))).map { res =>
        res
          .filter(_.isSuccess)
          .map {
            case MusitSuccess(maybeSuccess) =>
              maybeSuccess.map(dto => success(dto))

            case impossible =>
              throw new IllegalStateException("Encountered impossible state")

          }
          .filter(_.isDefined)
          .map(_.get)
      }
    }
  }

  /**
   * Fetch the {{{limit}}} number of last MoveObject events for the given
   * object ID.
   *
   * @param objectId the object ID to get move events for
   * @param limit    Int specifying the number of move events to get
   * @return A Future of a collection of MoveNode events.
   */
  def getObjectLocationHistory(
      mid: MuseumId,
      objectId: ObjectId,
      limit: Option[Int]
  ): Future[Seq[MoveObject]] = {
    eventsForObject(mid, objectId, MoveObjectType, limit) { dto =>
      MoveConverters.moveObjectFromDto(dto.asInstanceOf[BaseEventDto])
    }
  }

}
