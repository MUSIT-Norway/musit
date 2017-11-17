package services.conservation

import com.google.inject.Inject
import models.conservation.events.{ConservationEvent, ConservationModuleEvent}
import no.uio.musit.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.ConservationEventDao

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import no.uio.musit.time.dateTimeNow

abstract class ConservationEventService[T <: ConservationEvent: ClassTag] @Inject()(
    implicit
    val dao: ConservationEventDao[T],
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationEventService[T]])

  /**
   * Add a new conservation event
   */
  def add(mid: MuseumId, ce: T)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {

    /*val event: T =
      ce.withRegisteredInfo(Some(currUser.id), Some(dateTimeNow)).asInstanceOf[T]*/
    val res = for {
      added <- dao.insert(mid, ce)
      a <- dao
            .findSpecificConservationEventById(mid, added)
            .map(m => m.asInstanceOf[Option[ConservationEvent]])
    } yield a
    res
  }

  /**
   * Helper method specifically for adding an Analysis.
    ***/
  private def addConservationEvent(
      mid: MuseumId,
      ce: T
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {
    dao.insert(mid, ce)
  }

  /**
   * Locate an event with the given EventId.
   */
  def findConservationEventById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[T]] = {
    dao.findSpecificConservationEventById(mid, id)
  }

  /**
   * Update a conservationEvent
   */
  def update(
      mid: MuseumId,
      eventId: EventId,
      event: ConservationEvent
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {

    //val eventToWriteToDb = event.withUpdatedInfo(Some(currUser.id), Some(dateTimeNow))
    val updateRes = dao.update(mid, eventId, event)
    updateRes
    //TODO: I don't like to return 204-NoContent back to the frontend if something strange happened in the database on reading the event back in from the database!
    // I rather want 500 error. To fix this, we need a modified variant of updateRequestOpt and something equivalent to the below:
    //
    // futureMusitResultFoldNone(updateRes, MusitInternalError("Unable to get the updated event back from the database!"))

    /*
      TODO: Do we need to read the previous one from the database? Or isn't that any safer because the front-end
      is supposed to be able to override/clear anything not mentioned in its input json.
       The old variant below did something like this...


      val res = for {
        maybeEvent <- MusitResultT(
          dao.findConservationProcessById(mid, eventId)
        )
        maybeUpdated <- MusitResultT(
          maybeEvent.map { e =>
            val u = localupdateConservationProcess(e, cp)
            dao.update(mid, eventId, u)
          }.getOrElse(Future.successful(MusitSuccess(None)))
        )
      } yield maybeUpdated
      res.value
    }
   */

  }
}
