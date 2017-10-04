package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.{ConservationModuleEvent, TechnicalDescription}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TechnicalDescriptionDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers {

  val logger = Logger(classOf[TechnicalDescriptionDao])

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
    } yield maybeEvent

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
  def findTechnicalDescriptionById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[TechnicalDescription]]] = {
    findById(mid, id).map { r =>
      r.map { mc =>
        mc.flatMap {
          case ce: TechnicalDescription => Some(ce)
          case _                        => None
        }
      }
    }
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
      ce: TechnicalDescription
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] = {
    insertEvent(mid, ce)(asRow)
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
//  def update(
//              mid: MuseumId,
//              id: EventId,
//              cp: ConservationProcess
//            )(
//              implicit currUsr: AuthenticatedUser
//            ): Future[MusitResult[Option[ConservationProcess]]] = {
//    val action = updateAction(mid, id, cp).transactionally
//
//    db.run(action)
//      .flatMap { numUpdated =>
//        if (numUpdated == 1) {
//          findConservationProcessById(mid, id)
//        } else {
//          Future.successful {
//            MusitValidationError(
//              message = "Unexpected number of conservation process rows were updated.",
//              expected = Option(1),
//              actual = Option(numUpdated)
//            )
//          }
//        }
//      }
//      .recover(
//        nonFatal(s"An unexpected error occurred inserting an conservation process event")
//      )
//  }
//
//  private def updateAction(
//                            mid: MuseumId,
//                            id: EventId,
//                            event: ConservationProcess
//                          )(implicit currUsr: AuthenticatedUser): DBIO[Int] = {
//    eventTable.filter(_.eventId === id).update(asRow(mid, event))
//  }
}
