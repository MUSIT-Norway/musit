package services.conservation

import com.google.inject.Inject
import models.conservation.events.ConservationEvent
import no.uio.musit.MusitResults.MusitValidationError
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{ActorDate, EventId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import repositories.conservation.dao.ConservationEventDao
import services.conservation.EventSituation._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

abstract class ConservationEventService[T <: ConservationEvent: ClassTag] @Inject()(
    implicit
    val dao: ConservationEventDao[T],
    val conservationService: ConservationService,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationEventService[T]])

  /**
   * Add a new conservation event
   */
  def add(mid: MuseumId, ce: T)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationEvent]] = {
    for {

      _ <- conservationService.checkTypeOfObjects(ce.affectedThings.getOrElse(Seq.empty))

      newEvent <- conservationService
                   .updateSubEventWithDateAndActor(
                     mid,
                     ce,
                     Insert,
                     ActorDate(currUser.id, dateTimeNow)
                   )
                   .map(_.cleanupBeforeInsertIntoDatabase)
      added <- dao.insert(mid, newEvent.asInstanceOf[T])
      a <- dao
            .findSpecificConservationEventById(mid, added)
            .map(m => m.asInstanceOf[Option[ConservationEvent]])
    } yield a
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
    for {
      _ <- conservationService.checkTypeOfObjects(
            event.affectedThings.getOrElse(Seq.empty)
          )
      _ <- FutureMusitResult.requireFromClient(
            Some(eventId) == event.id,
            s"Inconsistent eventid in url($eventId) vs body (${event.id})"
          )
      newEvent <- conservationService
                   .updateSubEventWithDateAndActor(
                     mid,
                     event,
                     UpdateSelf,
                     ActorDate(currUser.id, dateTimeNow)
                   )
                   .map(_.cleanupBeforeInsertIntoDatabase)
      updateRes <- dao.update(mid, eventId, newEvent)
    } yield updateRes
  }

}
