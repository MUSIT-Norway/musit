package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, EventTypeId, MuseumId, ObjectUUID}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConservationProcessDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ec: ExecutionContext,
    val treatmentDao: TreatmentDao,
    val technicalDescriptionDao: TechnicalDescriptionDao
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[ConservationProcessDao])

  import profile.api._

  private def findByIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[ConservationModuleEvent]] = {
    val q1 = super.findByIdAction(mid, id).map { mayBeRow =>
      mayBeRow.map { row =>
        fromRow(row._1, row._7, row._10.flatMap(ObjectUUID.fromString), row._13).get
      }
    }
    /*val q1 = eventTable.filter(a => a.eventId === id && a.museumId === mid)
    q1.result.headOption.map { x =>
      x.flatMap { row =>
        fromRow(row._1, row._7, row._10.flatMap(ObjectUUID.fromString), row._13)
      }
    }*/
    q1
  }

  /**Creates an insert action for a subevent.*/
  def createInsertSubEventAction(
      mid: MuseumId,
      partOf: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[EventId] = {

    val dao = event match {
      case t: Treatment             => treatmentDao
      case td: TechnicalDescription => technicalDescriptionDao
    }
    dao.createInsertAction(mid, partOf, event)
  }

  /**
   * Locates a specific analysis module related event by its EventId.
   *
   * @param mid           the MuseumId to look for.
   * @param id            the EventId to look for.
   * @return eventually returns a MusitResult that might contain the ConservationModuleEvent.
   */
  def findById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[ConservationModuleEvent]]] = {
    val query = for {
      maybeEvent <- findByIdAction(mid, id)
      children   <- listChildrenAction(mid, id)

    } yield
      maybeEvent.map(
        event => event.asInstanceOf[ConservationProcess].copy(events = Some(children))
      )

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching event $id"))
  }

  /**
   * Same as findById, but will ensure that only the conservation specific events
   * are returned.
   *
   * @param id The event ID to look for
   * @return the Conservation that was found or None
   */
  def findConservationProcessById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[ConservationProcess]]] = {
    findById(mid, id).map { r =>
      r.map { mc =>
        mc.flatMap {
          case ce: ConservationProcess => Some(ce)
          case _                       => None
        }
      }
    }
  }

  private def readSubEvent(eventTypeId: EventTypeId, row: EventRow): ConservationEvent = {
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

    dao.interpretRow(row)
  }

  /**
   * Find all children events of a given conservationEvent
   *
   * Children are subtypes of ConservationEvent
   */
  private def listChildrenAction(
      mid: MuseumId,
      parentEventId: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Seq[ConservationEvent]] = {

    val query = eventTable.filter { a =>
      //TODO: Er det riktig å filtrere på museumId her?
      // I den grad det gir mening å ha subevents på tvers av museer er det kanskje tryggest å få de med ut her?

      a.partOf === parentEventId && a.museumId === mid
    }

    val action = query.result.map { res =>
      res.map { row =>
        readSubEvent(row._2, row)
      }
    }
    action
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {

    val subEvents = ce.events.getOrElse(Seq.empty)

    def subEventActions(partOf: EventId) =
      subEvents.map(
        subEvent =>
          createInsertSubEventAction(mid, partOf, subEvent.asPartOf(Some(partOf)))
      )

    //TODO: This is probably what we really want
    val cpToInsert = ce.withoutChildren

    //TODO: This is only temporary, to get the tests to work until we have a proper composite GET working
    //val cpToInsert = ce

    val actions: DBIO[EventId] = for {

      cpId <- insertAction(asRow(mid, cpToInsert))
      _    <- DBIO.sequence(subEventActions(cpId)).map(_ => cpId)

    } yield cpId

    db.run(actions.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred trying to add event"))
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
  ): Future[MusitResult[Option[ConservationProcess]]] = {
    val action = updateAction(mid, id, cp).transactionally

    db.run(action)
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findConservationProcessById(mid, id)
        } else {
          Future.successful {
            MusitValidationError(
              message = "Unexpected number of conservation process rows were updated.",
              expected = Option(1),
              actual = Option(numUpdated)
            )
          }
        }
      }
      .recover(
        nonFatal(s"An unexpected error occurred inserting an conservation process event")
      )
  }

  private def updateAction(
      mid: MuseumId,
      id: EventId,
      event: ConservationProcess
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    eventTable.filter(_.eventId === id).update(asRow(mid, event))
  }
}
