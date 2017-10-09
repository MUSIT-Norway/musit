package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.{ConservationEvent, ConservationModuleEvent}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.reflect._

class ConservationEventDao[T <: ConservationEvent: ClassTag] @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[ConservationEventDao[ConservationEvent]])

  import profile.api._

  protected def findByIdAction(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): DBIO[Option[ConservationEvent]] = {
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
    q1.map(x => x.map(e => e.asInstanceOf[ConservationEvent]))
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
  ): Future[MusitResult[Option[ConservationEvent]]] = {
    val query = for {
      maybeEvent <- findByIdAction(mid, id)
    } yield maybeEvent

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching event $id"))
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
  ): Future[MusitResult[Option[T]]] = {
    findById(mid, id).map { r =>
      r.map { mc =>
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
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {
    insertEvent(mid, ce)(asRow)
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
  ): Future[MusitResult[Option[ConservationEvent]]] = {
    val action = updateAction(mid, id, event).transactionally

    db.run(action)
      .flatMap { numUpdated =>
        if (numUpdated == 1) {
          findSpecificById(mid, id)
        } else {
          Future.successful {
            MusitValidationError(
              message = "Unexpected number of event rows were updated.",
              expected = Option(1),
              actual = Option(numUpdated)
            )
          }
        }
      }
      .recover(
        nonFatal(s"An unexpected error occurred updating the conservation event")
      )
  }

  private def updateAction(
      mid: MuseumId,
      id: EventId,
      event: ConservationEvent
  )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
    eventTable.filter(_.eventId === id).update(asRow(mid, event))
  }
}
