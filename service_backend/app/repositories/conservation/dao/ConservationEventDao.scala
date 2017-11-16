package repositories.conservation.dao

import com.google.inject.Inject
import controllers.conservation.MusitResultUtils._
import models.conservation.events.ConservationEvent
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID}
import no.uio.musit.musitUtils.Utils
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, _}

class ConservationEventDao[T <: ConservationEvent: ClassTag] @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val objectEventDao: ObjectEventDao,
    val daoUtils: DaoUtils
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[ConservationEventDao[ConservationEvent]])

  import profile.api._

  def interpretRow(row: EventRow): ConservationEvent = {
    fromRow(
      row._1, /*TODO?row._9,*/ row._7,
      row._10.flatMap(ObjectUUID.fromString),
      row._13
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

  protected def findByIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[ConservationEvent]] = {
    val q1 = super.findByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.map { row =>
        interpretRow(row)
      }
    }
    q1.flatMap {
      case (Some(event)) => {
        val futObjectList = getEventObjectsAction(id)
        val futObj = futObjectList.map { ol =>
          val lista = Some(event.withAffectedThings(Some(ol)))
          lista
        }
        futObj
      }
      case None =>
        DBIO.successful(None)
    }
  }

  /**
   * Locates a given conservation event by its EventId.
   *
   * @param mid           the MuseumId to look for.
   * @param id            the EventId to look for.
   * @return eventually returns a MusitResult that might contain the ConservationEvent.
   */
  def findById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {
    val query = for {
      maybeEvent <- findByIdAction(mid, id)
    } yield maybeEvent
    daoUtils.dbRun(query, s"An unexpected error occurred fetching event $id")
    /*db.run(query)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching event $id"))*/
  }

  /**
   * Same as findById, but will ensure that only events of the given/specific types
   * are returned.
   *
   * @param id The event ID to look for
   * @return the event that was found. Exception is thrown if wrong event type found.
   */
  def findSpecificById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[T]] = {
    findById(mid, id).map { mc =>
      mc.flatMap {
        case ce: T =>
          Some(ce)
        case x =>
          val expectedClassName = classTag[T].runtimeClass.getName()
          val foundClassName    = x.getClass().getName()
//            println("fant: " + foundClassName + " forventet: " + expectedClassName)
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

    def localFindById(id: EventId) = findById(mid, id)

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
    val action = createInsertAction(mid, ce.partOf, ce)
    daoUtils.dbRun(
      action.transactionally,
      s"An error occurred trying to add event ${ce.getClass.getName}"
    )
  }

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
    val objectIds           = event.affectedThings.getOrElse(Seq.empty)
    val eventWithoutObjects = event.withAffectedThings(None)
    val eventWithRegisteredDate =
      eventWithoutObjects.withRegisteredInfo(Some(currUsr.id), Some(dateTimeNow))
    val row    = asRow(mid, eventWithRegisteredDate)
    val newRow = row.copy(_9 = partOf)
    for {
      eventId <- insertAction(newRow)
      _       <- objectEventDao.insertObjectEventAction(eventId, objectIds)
    } yield eventId
  }

  def createUpdateAction(
      mid: MuseumId,
      partOf: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    require(event.id.isDefined)
    val eventId = event.id.get

    //updated and registered are filled in during putUpdateAndRegDataToProcessAndSubevents
    //or updated are filled in during conservationEvent.update(update on single subevent)
    require(event.updatedBy.get == currUsr.id)
    require(event.updatedDate.isDefined)

    require(event.registeredBy.isDefined)
    require(event.registeredDate.isDefined)

    val objectIds = event.affectedThings.getOrElse(Seq.empty)
    val row       = asRow(mid, event)
    val newRow    = row.copy(_9 = Some(partOf))

    for {
      numEventRowsUpdated <- updateActionRowOnly(mid, eventId, event)
      _                   <- objectEventDao.updateObjectEventAction(eventId, objectIds)
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
    val action       = createUpdateAction(mid, id, updatedEvent).transactionally
    //println("inniUpdate-dao " + event)

    daoUtils
      .dbRun(action, "An unexpected error occurred updating the conservation event")
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findSpecificById(mid, id).map(_.asInstanceOf[Option[T]])
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
      mid: MuseumId,
      id: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    eventTable.filter(_.eventId === id).update(asRow(mid, event))
  }

}
