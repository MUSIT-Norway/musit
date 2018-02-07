package services.conservation

import com.google.inject.Inject
import models.conservation.ConservationProcessKeyData
import models.conservation.events._
import no.uio.musit.MusitResults
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import repositories.conservation.dao._
import services.conservation.EventSituation.{
  EventSituation,
  Insert,
  PreserveDates,
  UpdateSelf
}

import scala.concurrent.{ExecutionContext, Future}

class ConservationProcessService @Inject()(
    implicit
    val conservationProcDao: ConservationProcessDao,
    val typeDao: ConservationTypeDao,
    val dao: ConservationDao,
    val objectDao: ObjectEventDao,
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

  //Only used from the Elastic Search indexing.
  def getAllEventTypes() = typeDao.allEventTypes()

  def addConservationProcess(
      mid: MuseumId,
      cp: ConservationProcess
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {
    val event =
      fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
        mid,
        cp,
        ActorDate(currUser.id, dateTimeNow),
        true
      ).map(_.cleanupBeforeInsertIntoDatabase)
    event.flatMap { ev =>
      conservationService
        .checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
        .flatMap(m => conservationProcDao.insert(mid, ev))
    }
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

  /*
   *This method filters on the attribute isUpdated(for the conservationProcess and its subEvents) for setting
   * the currentDate and currentUser in updated_date and updated_by in the subEvents that is updated.
   * This method returns the whole conservationprocess with the filtered subEvents with its updated
   * info. Since FrontEnd doesn't have a clue about registered_date, -by and
   * updated_date and by, we have to get some date/actors from the database.
   * */

  def fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
      mid: MuseumId,
      cp: ConservationProcess,
      actorDate: ActorDate,
      isInsert: Boolean
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[ConservationProcess] = {
    val mrSituation =
      cp.isUpdated match {
        case Some(true)  => MusitSuccess(if (isInsert) Insert else UpdateSelf)
        case Some(false) => MusitSuccess(PreserveDates)
        case None =>
          MusitValidationError("Missing property isUpdated on conservation process")
      }

    mrSituation match {
      case MusitSuccess(situation) =>
        val newCp =
          conservationService.updateProcessWithDateAndActor(mid, cp, situation, actorDate)
        val origChildren = cp.events.getOrElse(Seq.empty)

        val newSubEvents = newCp.flatMap { cp =>
          val newChildren =
            origChildren
              .filter(
                subEvent => subEvent.isUpdated.isDefined && subEvent.isUpdated.get == true
              )
              .map { subEvent =>
                val situation = subEvent.id.isDefined match {
                  case true  => UpdateSelf
                  case false => Insert
                }
                conservationService
                  .updateSubEventWithDateAndActor(mid, subEvent, situation, actorDate)
              }
          FutureMusitResult.sequence(newChildren)

        }
        newCp.flatMap(
          ncp => newSubEvents.map(subEventList => ncp.withEvents(subEventList))
        )

      //case err: MusitValidationError => FutureMusitResult.from(err)
      case err: MusitError => FutureMusitResult.from[ConservationProcess](err)
    }

  }

  /**
   * Update a conservationProcess
   */
  def update(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationProcess
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    for {
      _ <- FutureMusitResult.requireFromClient(
            Some(eventId) == cp.id,
            s"Inconsistent eventid in url($eventId) vs body (${cp.id})"
          )
      _ <- conservationService.checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
      newCp <- fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
                mid,
                cp,
                ActorDate(currUser.id, dateTimeNow),
                false
              ).map(_.cleanupBeforeInsertIntoDatabase)
      _            <- { conservationProcDao.update(mid, eventId, newCp) }
      maybeUpdated <- findConservationProcessById(mid, eventId)

    } yield maybeUpdated
  }

  def getConservationWithKeyDataForObject(mid: MuseumId, objectUuid: ObjectUUID)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationProcessKeyData]] = {

    val res1 = objectDao.getConservationProcessIdsAndCaseNumbersForObject(objectUuid)
    val res2 = res1.flatMap { seqTuple =>
      val res3 = seqTuple.map { m =>
        objectDao.getEventsForSpecificCpAndObject(m._1, objectUuid).map { eid =>
          ConservationProcessKeyData(
            m._1.underlying,
            m._2,
            m._3,
            m._4,
            Some(eid.map(_._1)),
            Some(eid.map(_._2))
          )
        }

      }
      FutureMusitResult.sequence(res3)

    }
    res2

  }

}
