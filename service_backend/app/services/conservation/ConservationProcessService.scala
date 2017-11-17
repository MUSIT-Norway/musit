package services.conservation

import com.google.inject.Inject
import controllers.conservation.MusitResultUtils
import models.conservation.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import repositories.conservation.dao.{ConservationProcessDao, ConservationTypeDao}

import scala.concurrent.{ExecutionContext, Future}

class ConservationProcessService @Inject()(
    implicit
    val conservationDao: ConservationProcessDao,
    val typeDao: ConservationTypeDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationProcessService])

  def getTypesFor(coll: Option[CollectionUUID])(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[ConservationType]]] = {
    typeDao.allFor(coll)
  }

  def addConservationProcess(
      mid: MuseumId,
      cp: ConservationProcess
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {
    val event = cp.withRegisteredInfo(Some(currUser.id), Some(dateTimeNow))
    conservationDao.insert(mid, event)
  }

  /**
   * Locate an event with the given EventId.
   */
  def findConservationProcessById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    def findSubEvent(v: EventIdWithEventTypeId) =
      conservationDao.readSubEvent(v.eventTypeId, mid, v.eventId)

    val futOptCp = conservationDao.findConservationProcessIgnoreSubEvents(mid, id)
    futOptCp.flatMapInsideOption { cp =>
      for {
        childrenIds <- conservationDao.listSubEventIdsWithTypes(mid, id)
        subEvents <- FutureMusitResult.collectAllOrFail(
                      childrenIds,
                      findSubEvent,
                      (failedIdWithTypes: Seq[EventIdWithEventTypeId]) =>
                        MusitValidationError(
                          s"unable to find subevents with ids and types: $failedIdWithTypes of event with id: $id"
                      )
                    )
      } yield cp.copy(events = Some(subEvents))
    }
  }

  def putUpdateAndRegDataToProcessAndSubevents(
      conservationProcess: ConservationProcess,
      currUser: ActorId,
      currDate: DateTime,
      findRegisteredActorDate: EventId => FutureMusitResult[ActorDate]
  ): FutureMusitResult[ConservationProcess] = {
    val cp = conservationProcess.withUpdatedInfo(Some(currUser), Some(currDate))
    val subevents = cp.events
      .getOrElse(Seq.empty)
      .map(event => {
        event.id match {
          case Some(id) =>
            findRegisteredActorDate(id).map { dbEventActorDate =>
              event
                .withUpdatedInfo(Some(currUser), Some(currDate))
                .withRegisteredInfo(
                  Some(dbEventActorDate.user),
                  Some(dbEventActorDate.date)
                )
            }
          case None =>
            FutureMusitResult
              .from(event.withRegisteredInfo(Some(currUser), Some(currDate)))
        }
      })

    val newSubevents = FutureMusitResult.sequence(subevents)
    val res = for {
      processActorDate <- findRegisteredActorDate(cp.id.get)
      events           <- newSubevents

    } yield
      cp.withRegisteredInfo(Some(processActorDate.user), Some(processActorDate.date))
        .withEvents(events)
    res
  }

  /**
   * Update an conservationProcess
   */
  import controllers.conservation.MusitResultUtils._

  def update(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationProcess
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {
    import no.uio.musit.functional.Extensions

    def getRegisteredActorDate(localEventId: EventId): FutureMusitResult[ActorDate] = {
      conservationDao
        .findRegisteredActorDate(mid, localEventId)
        .getOrError(
          MusitValidationError(
            s"Unable to find conservation subevent with id (trying to find registered by/date): $eventId"
          )
        )
    }

    for {
      _ <- FutureMusitResult.requireFromClient(
            Some(eventId) == cp.id,
            s"Inconsistent eventid in url($eventId) vs body (${cp.id})"
          )

      eventToWriteToDb <- putUpdateAndRegDataToProcessAndSubevents(
                           cp,
                           currUser.id,
                           dateTimeNow,
                           getRegisteredActorDate
                         )

      _            <- conservationDao.update(mid, eventId, eventToWriteToDb)
      maybeUpdated <- findConservationProcessById(mid, eventId)

    } yield maybeUpdated

  }

}
