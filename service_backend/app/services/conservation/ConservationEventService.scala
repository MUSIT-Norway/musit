package services.conservation

import com.google.inject.Inject
import models.conservation.events.{ConservationEvent, ConservationModuleEvent}
import no.uio.musit.MusitResults.{
  MusitInternalError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
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
import no.uio.musit.functional.Extensions._

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
    val event: T =
      ce.withRegisteredInfo(Some(currUser.id), Some(dateTimeNow)).asInstanceOf[T]
    val res = for {
      added <- dao.insert(mid, event)
      a <- dao
            .findSpecificConservationEventById(mid, added)
            .map(m => m.asInstanceOf[Option[ConservationEvent]])
    } yield a
    res
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

    val regActorDate = dao
      .findRegisteredActorDate(mid, eventId)
      .getOrError(
        MusitValidationError(
          s"Unable to find conservation subevent with id (trying to find registered by/date): $eventId"
        )
      )

    for {
      actorDate <- regActorDate

      eventToWriteToDb = event
        .withUpdatedInfo(Some(currUser.id), Some(dateTimeNow))
        .withRegisteredInfo(Some(actorDate.user), Some(actorDate.date))

      updateRes <- dao.update(mid, eventId, eventToWriteToDb)

    } yield updateRes

  }

}
