package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.MusitResults.MusitValidationError
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext

@Singleton
class ConservationProcessDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ec: ExecutionContext,
    val treatmentDao: TreatmentDao,
    val technicalDescriptionDao: TechnicalDescriptionDao,
    val daoUtils: DaoUtils
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[ConservationProcessDao])

  import profile.api._

  def subEventDao = treatmentDao //Arbitrary choice, could have used any of the other.

  def interpretConservationProcessRow(row: EventRow): ConservationProcess = {
    require(
      valEventTypeId(row) == ConservationProcess.eventTypeId,
      s"didn't get proper conservationProcess eventTypeId, expected: ${ConservationProcess.eventTypeId}, found: ${valEventTypeId(row)}"
    )

    fromRow(
      valEventId(row), /*TODO?row._9,*/ valDoneDate(row),
      valAffectedThing(row).flatMap(ObjectUUID.fromString),
      valJson(row)
    ).get.asInstanceOf[ConservationProcess]
  }

  private def findConservationProcessByIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[ConservationProcess]] = {
    val q1 = super.findByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.map { row =>
        interpretConservationProcessRow(row)
      }
    }
    q1
  }

  private def getDaoFor(event: ConservationEvent) = {
    event match {
      case t: Treatment             => treatmentDao
      case td: TechnicalDescription => technicalDescriptionDao
    }
  }

  /**Creates an insert action for a subevent.*/
  def createInsertSubEventAction(
      mid: MuseumId,
      partOf: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[EventId] = {

    val dao = getDaoFor(event)
    dao.createInsertAction(mid, Some(partOf), event)
  }

  /**Creates an insert or update action for a subevent, an update if it has an eventId, else an insert.*/
  def createInsertOrUpdateSubEventAction(
      mid: MuseumId,
      partOf: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[EventId] = {

    val dao = getDaoFor(event)
    event.id match {
      case Some(id) =>
        dao.createUpdateAction(mid, event).map { numUpdated =>
          numUpdated match {
            case 0 => throw new MusitSlickClientError("klientfeil")
            case 1 => id
            case _ =>
              throw new Exception(s"too many rows in update of eventId $id")
          }
        }
      case None => dao.createInsertAction(mid, Some(partOf), event)
    }
  }

  /**
   * Reads in the conservation process, but without subEvents!!!
   * @param id The event ID to look for
   * @return the Conservation that was found or None
   */
  def findConservationProcessIgnoreSubEvents(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {
    val query = findConservationProcessByIdAction(mid, id)

    daoUtils.dbRun(
      query,
      s"An unexpected error occurred fetching conservation process event $id"
    )
  }

  def readSubEvent(
      eventTypeId: EventTypeId,
      mid: MuseumId,
      eventId: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {
    val optSubEventType = ConservationEventType(eventTypeId)
    val subEventType = optSubEventType.getOrElse(
      throw new IllegalStateException(
        s"Unhandled/unknown eventTypeId: $eventTypeId in ConservationProcessDao.readSubEvent"
      )
    )

    val dao = subEventType match {
      case Treatment            => treatmentDao
      case TechnicalDescription => technicalDescriptionDao
    }
    val subEvent = dao.findConservationEventById(mid, eventId)
    subEvent
  }

  /**
   * Find all children events of a given conservationEvent
   *
   * Children are subtypes of ConservationEvent
   */
  def listSubEventIdsWithTypes(
      mid: MuseumId,
      parentEventId: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Seq[EventIdWithEventTypeId]] = {

    val query = eventTable.filter { a =>
      //TODO: Er det riktig å filtrere på museumId her?
      // I den grad det gir mening å ha subevents på tvers av museer er det kanskje tryggest å få de med ut her?

      a.partOf === parentEventId && a.museumId === mid
    }
    val action = query.result.map { res =>
      res.map { row =>
        EventIdWithEventTypeId(valEventId(row).get, valEventTypeId(row))

      }
    }

    daoUtils.dbRun(
      action,
      s"An error occurred trying to read sub events of event with id: $parentEventId"
    )
  }

  /**
   * Write a single {{{Conservation}}} event to the DB.
   *
   * @param mid the MuseumId
   * @param ce  The Conservation to persist.
   * @return eventually returns a MusitResult containing the EventId.
   */
  def insert(
      mid: MuseumId,
      ce: ConservationProcess
  )(implicit currUsr: AuthenticatedUser): FutureMusitResult[EventId] = {

    val subEvents = ce.events.getOrElse(Seq.empty)

    def subEventActions(partOf: EventId) =
      subEvents.map(
        subEvent => {
          //val event = subEvent.withRegisteredInfo(Some(currUsr.id), Some(dateTimeNow))
          createInsertSubEventAction(mid, partOf, subEvent.asPartOf(Some(partOf)))
        }
      )

    val cpToInsert = ce.withoutChildren
    val actions: DBIO[EventId] = for {

      cpId <- insertAction(asRow(mid, cpToInsert))
      _    <- DBIO.sequence(subEventActions(cpId)).map(_ => cpId)

    } yield cpId

    daoUtils.dbRun(actions.transactionally, "An error occurred trying to add event")

  }

  /**
   * Performs an update action against the DB using the values in the provided
   * {{{ConservationProcess}}} argument.
   *
   * @param mid the MuseumId
   * @param id  the EventId associated with the analysis event to update
   * @param cp  the ConservationProcess to update
   * @return a result with an option of the updated event
   */
  def update(
      mid: MuseumId,
      id: EventId,
      cp: ConservationProcess
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Unit] = {
    val subEvents = cp.events.getOrElse(Seq.empty)

    def subEventActions(partOf: EventId): Seq[DBIO[EventId]] = {
      subEvents.map(
        subEvent => {
          createInsertOrUpdateSubEventAction(mid, partOf, subEvent.asPartOf(Some(partOf)))
        }
      )
    }

    //We "clear" the children so that we don't get them embedded in the json-blob for the process
    val cpToInsert = cp.withoutChildren
    //val cpWithoutActorsToInsert = cpToInsert.withoutActorRoleAndDates

    val actions: DBIO[Int] = for {
      numUpdated <- updateAction(mid, id, cpToInsert)
      _          <- DBIO.sequence(subEventActions(id)).map(_ => 1)
    } yield numUpdated

    daoUtils
      .dbRun(
        actions.transactionally,
        "An unexpected error occurred inserting an conservation process event"
      )
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          FutureMusitResult.from(())
          //#OLD findConservationProcessWithoutSubEvents(mid, id)
        } else {
          FutureMusitResult.failed {
            MusitValidationError(
              message = "Unexpected number of conservation process rows were updated.",
              expected = Option(1),
              actual = Option(numUpdated)
            )
          }
        }
      }

  }

  private def updateAction(
      mid: MuseumId,
      id: EventId,
      event: ConservationProcess
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    eventTable.filter(_.eventId === id).update(asRow(mid, event))
  }
}
