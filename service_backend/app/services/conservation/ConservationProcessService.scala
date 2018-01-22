package services.conservation

import com.google.inject.Inject
import models.conservation.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import repositories.conservation.dao.{
  ConservationDao,
  ConservationProcessDao,
  ConservationTypeDao,
  TreatmentDao
}

import scala.concurrent.{ExecutionContext, Future}

class ConservationProcessService @Inject()(
    implicit
    val conservationProcDao: ConservationProcessDao,
    val typeDao: ConservationTypeDao,
    val dao: ConservationDao,
    val subEventDao: TreatmentDao, //Arbitrary choice, to get access to helper functions irrespective of event type
    //Should have been ConservationModuleEventDao (TODO: Make this split)
    val conservationService: ConservationService,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationProcessService])

  def getTypesFor(coll: Option[CollectionUUID])(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationType]] = {
    typeDao.allFor(coll)
  }

  def addConservationProcess(
      mid: MuseumId,
      cp: ConservationProcess
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {
    val event = copyWithRegDataForProcessAndSubEvents(cp, currUser.id, dateTimeNow)
    conservationService
      .checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
      .flatMap(m => conservationProcDao.insert(mid, event))
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
      conservationProcDao.readSubEvent(v.eventTypeId, mid, v.eventId)

    val futOptCp = conservationProcDao.findConservationProcessIgnoreSubEvents(mid, id)
    futOptCp.flatMapInsideOption { cp =>
      for {
        childrenIds <- conservationProcDao.listSubEventIdsWithTypes(mid, id)
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

  /*** Fill in registered by and date both in the process and in all subevents */
  def copyWithRegDataForProcessAndSubEvents(
      conservationProcess: ConservationProcess,
      currUser: ActorId,
      currDate: DateTime
  ): ConservationProcess = {

    val someCurrUser = Some(currUser)
    val someCurrDate = Some(currDate)

    val cp = conservationProcess.withRegisteredInfo(someCurrUser, someCurrDate)

    val subEvents = cp.events
      .getOrElse(Seq.empty)
      .map(subEvent => subEvent.withRegisteredInfo(someCurrUser, someCurrDate))

    cp.withEvents(subEvents)

  }

  /***
   *  Fill in updated by and date in the process and set updated and
   *  registered by/date as appropriate in the subevents */
  def copyWithUpdateAndRegDataToProcessAndSubEvents(
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
  def update(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationProcess
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    conservationService
      .checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
      .flatMap { m =>
        def getRegisteredActorDate(
            localEventId: EventId
        ): FutureMusitResult[ActorDate] = {
          subEventDao
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

          eventToWriteToDb <- copyWithUpdateAndRegDataToProcessAndSubEvents(
                               cp,
                               currUser.id,
                               dateTimeNow,
                               getRegisteredActorDate
                             )

          _            <- conservationProcDao.update(mid, eventId, eventToWriteToDb)
          maybeUpdated <- findConservationProcessById(mid, eventId)

        } yield maybeUpdated
      }
  }

}
