package repositories.conservation.dao

import com.google.inject.Inject
import models.conservation.events.{ConservationEvent, ConservationProcess}
import no.uio.musit.MusitResults.MusitValidationError
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext
import scala.reflect.{ClassTag, _}

class ConservationEventDao[T <: ConservationEvent: ClassTag] @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val objectEventDao: ObjectEventDao,
    val daoUtils: DaoUtils,
    val actorRoleDao: ActorRoleDateDao,
    val eventDocumentDao: EventDocumentDao
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[ConservationEventDao[ConservationEvent]])

  import profile.api._

  /** Gets the registered by and date fields. Handles both processed and subevents. */
  def findRegisteredActorDate(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ActorDate]] = {

    val query = super.findByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.map { row =>
        ActorDate(valRegisteredBy(row), valRegisteredDate(row))
      }
    }
    daoUtils.dbRun(
      query,
      s"An unexpected error occurred fetching registered by/date for event $id"
    )
  }

  /** Gets the updated by and date fields. Handles both processed and subevents. */
  def findUpdatedActorDate(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ActorDate]] = {

    val query = super.findByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.flatMap { row =>
        valUpdatedBy(row) match {
          case Some(actor) =>
            Some(ActorDate(valUpdatedBy(row).get, valUpdatedDate(row).get))
          case None => None
        }
      }
    }
    daoUtils.dbRun(
      query,
      s"An unexpected error occurred fetching updated by/date for event $id"
    )
  }

  def interpretRow(row: EventRow): ConservationEvent = {
    fromConservationRow(
      valEventId(row),
      valJson(row)
    ).get.asInstanceOf[ConservationEvent]
  }

  /*Todo: Add getOrElse instead of the above .get?
        .getOrElse(
        throw new IllegalStateException(
          s"Unable to read event row from the database, with row: $row in ConservationProcessDao.readSubEvent"
        )
      )

   */

  def getEventObjectsAction(eventId: EventId): DBIO[Seq[ObjectUUID]] = {
    objectEventDao.getEventObjectsAction(eventId)

  }

  def getObjectEventIds(objectUuid: ObjectUUID): FutureMusitResult[Seq[EventId]] = {
    objectEventDao.getObjectEventIds(objectUuid)

  }

  protected def localFindByIdAction(
      mid: MuseumId,
      id: EventId
  ): DBIO[Option[EventRow]] =
    eventTable.filter { e =>
      e.museumId === mid &&
      e.eventId === id &&
      e.eventTypeId =!= ConservationProcess.eventTypeId &&
      e.isDeleted === 0
    }.result.headOption

  def findConservationEventByIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[ConservationEvent]] = {
    val q1 = localFindByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.map { row =>
        interpretRow(row)
      }
    }
    q1
  }

  def enrichWithSpecialAttributes(
      eventId: EventId,
      event: ConservationEvent
  ): FutureMusitResult[ConservationEvent] =
    FutureMusitResult.successful(event)

  /**
   * Locates a given conservation event by its EventId.
   *
   * @param mid           the MuseumId to look for.
   * @param id            the EventId to look for.
   * @return eventually returns a MusitResult that might contain the ConservationEvent.
   */
  def findConservationEventById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {
    val query = findConservationEventByIdAction(mid, id)
    daoUtils
      .dbRun(
        query,
        s"An unexpected error occurred fetching event $id (Scala type: ${classTag[T].runtimeClass.getName()}"
      )
      .flatMapInsideOption { event =>
        {
          for {
            objects       <- objectEventDao.getEventObjects(id)
            actors        <- actorRoleDao.getEventActorRoleDates(id)
            documents     <- eventDocumentDao.getDocuments(id)
            enrichedEvent <- enrichWithSpecialAttributes(id, event)
          } yield
            enrichedEvent
              .withAffectedThings(Some(objects))
              .withActorRoleAndDates(Some(actors))
              .withDocuments(Some(documents))
        }
      }
  }

  /**
   * Same as findById, but will ensure that only events of the given/specific types
   * are returned.
   *
   * @param id The event ID to look for
   * @return the event that was found. Exception is thrown if wrong event type found.
   */
  def findSpecificConservationEventById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[T]] = {
    findConservationEventById(mid, id).map { mc =>
      mc.flatMap {
        case ce: T =>
          Some(ce)
        case x =>
          val expectedClassName = classTag[T].runtimeClass.getName()
          val foundClassName    = x.getClass().getName()
          throw new IllegalStateException(
            s"findEventById, expected to find an event of a given type: ${expectedClassName}" +
              s", but it had another type: ${foundClassName}"
          )

      }
    }
  }

  def getEventsForObject(mid: MuseumId, objectUuid: ObjectUUID)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationEvent]] = {

    def localFindById(id: EventId) = findConservationEventById(mid, id)
    for {
      ids <- getObjectEventIds(objectUuid)
      events <- {
        FutureMusitResult.collectAllOrFail[EventId, ConservationEvent](
          ids,
          localFindById,
          eventIds => MusitValidationError(s"Missing events for these eventIds:$eventIds")
        )
      }
    } yield events
  }

  /**
   * Write a single {{{ConservationEvent}}} to the DB.
   *
   * @param mid the MuseumId
   * @param ce  The event to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insert(
      mid: MuseumId,
      ce: T
  )(implicit currUsr: AuthenticatedUser): FutureMusitResult[EventId] = {
    val event  = ce.withRegisteredInfo(Some(currUsr.id), Some(dateTimeNow))
    val action = createInsertAction(mid, event.partOf, event)
    daoUtils.dbRun(
      action.transactionally,
      s"An error occurred trying to add event ${ce.getClass.getName}"
    )
  }

  /** Removes special stuff for this event type which we don't want to include in the json-column in the database
   * (This stuff gets stored in the database separately, probably in another table (likely one-to-many))
   *
   */
  def removeSpecialEventAttributes(event: ConservationEvent): ConservationEvent = event

  def insertSpecialAttributes(eventId: EventId, event: ConservationEvent): DBIO[Unit] =
    DBIO.successful(())

  def updateSpecialAttributes(eventId: EventId, event: ConservationEvent): DBIO[Unit] =
    DBIO.successful(())

  /**
   * an insert action for inserting a conservationEvent
   * eventWithoutObjects is the event without it's list
   * of objectIds. We will not save the objectList in the
   * event's json. The objects are saved in the table objectEvent.
   *
   * @param mid MuseumId
   * @param partOf the eventId of the conservationProcess
   * @param event the event to be inserted.
   * @return a DBIO[EventId] the eventId of the ConservationEvent(subEvent)
   */
  def createInsertAction(
      mid: MuseumId,
      partOf: Option[EventId],
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[EventId] = {
    // registered by and date are filled in in the service layer (during fillAppropriateblablabla)
    require(event.registeredBy.isDefined)
    require(event.registeredDate.isDefined)

    val objectIds      = event.affectedThings.getOrElse(Seq.empty)
    val actorsAndRoles = event.actorsAndRoles.getOrElse(Seq.empty)
    val documents      = event.documents.getOrElse(Seq.empty)

    val eventWithoutActorRoleAndDate = event.withoutActorRoleAndDates
    val eventWithoutObjects          = eventWithoutActorRoleAndDate.withAffectedThings(None)
    val eventWithoutDocument         = eventWithoutObjects.withDocuments(None)

    val fullyTrimmedEvent = removeSpecialEventAttributes(eventWithoutDocument)
    val row               = asRow(mid, fullyTrimmedEvent)
    val newRow            = withPartOf(row, partOf)
    for {
      eventId <- insertAction(newRow)
      sa      <- insertSpecialAttributes(eventId, event) //We use event because we need to send in the unmodified event!
      actors  <- actorRoleDao.insertActorRoleDateAction(eventId, actorsAndRoles)
      objects <- objectEventDao.insertObjectEventAction(eventId, objectIds)
      _       <- eventDocumentDao.insertDocumentAction(eventId, documents)
    } yield eventId
  }

  def createUpdateAction(
      mid: MuseumId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    require(event.id.isDefined)
    val eventId = event.id.get
    //updated and registered are filled in during copyWithUpdateAndRegDataToProcessAndSubevents
    //or otherwise in the service layer (update on single subevent)
    //require(event.updatedBy.get == currUsr.id)
    require(event.updatedBy.isDefined)
    require(event.updatedDate.isDefined)

    require(event.registeredBy.isDefined)
    require(event.registeredDate.isDefined)

    val objectIds                    = event.affectedThings.getOrElse(Seq.empty)
    val actors                       = event.actorsAndRoles.getOrElse(Seq.empty)
    val documents                    = event.documents.getOrElse(Seq.empty)
    val eventWithoutActorRoleAndDate = event.withoutActorRoleAndDates
    val eventWithoutObjects          = eventWithoutActorRoleAndDate.withAffectedThings(None)
    val eventWithoutDocuments        = eventWithoutObjects.withDocuments(None)
    val fullyTrimmedEvent            = removeSpecialEventAttributes(eventWithoutDocuments)
    val row                          = asRow(mid, fullyTrimmedEvent)
    for {
      numEventRowsUpdated <- updateActionRowOnly(eventId, row)
      _                   <- updateSpecialAttributes(eventId, event) //We use event because we need to send in the unmodified event!
      _                   <- objectEventDao.updateObjectEventAction(eventId, objectIds)
      _                   <- actorRoleDao.updateActorRoleDateAction(eventId, actors)
      _                   <- eventDocumentDao.updateDocumentAction(eventId, documents)
    } yield numEventRowsUpdated

  }

  /**
   * Performs an update action against the DB using the values in the provided
   * {{{ConservationEvent}}} argument.
   *
   * @param mid the MuseumId
   * @param id  the EventId associated with the analysis event to update
   * @param event  the event to update
   * @return a result with an option of the updated event
   */
  def update(
      mid: MuseumId,
      id: EventId,
      event: ConservationEvent
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {
    val updatedEvent = event.withUpdatedInfo(Some(currUsr.id), Some(dateTimeNow))

    val action = createUpdateAction(mid, updatedEvent).transactionally

    daoUtils
      .dbRun(action, "An unexpected error occurred updating the conservation event")
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findSpecificConservationEventById(mid, id).map(_.asInstanceOf[Option[T]])
        } else {
          FutureMusitResult.failed {
            MusitValidationError(
              message = "Unexpected number of event rows were updated.",
              expected = Option(1),
              actual = Option(numUpdated)
            )
          }
        }
      }
  }

  private def updateActionRowOnly(
      id: EventId,
      event: EventRow
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    eventTable.filter(e => e.eventId === id && e.isDeleted === 0).update(event)
  }

  def getCurrentEventForSpecificEventType(
      oUuid: ObjectUUID,
      eventTypeId: EventTypeId
  ): FutureMusitResult[Option[EventId]] = {
    val uuid = oUuid.asString
    val eventId =
      sql"""select max(e.event_id) from MUSARK_CONSERVATION.OBJECT_EVENT o, MUSARK_CONSERVATION.event e
           where o.object_uuid =${uuid} and o.event_id = e.event_id and e.is_deleted = 0
           and e.type_id = ${eventTypeId.underlying}
         """.as[Long].headOption
    daoUtils
      .dbRun(eventId, "Unexpected error in getCurrentEventForSpecificEventType")
      .map(optLong => optLong.map(l => EventId(l)))
  }

}
