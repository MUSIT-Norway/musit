package services.conservation

import com.google.inject.Inject
import models.conservation.events.{ConservationEvent, ConservationProcess, EventRole}
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.functional.Extensions._
import play.api.Logger
import repositories.conservation.dao.{ActorRoleDateDao, ConservationDao, TreatmentDao}
import services.conservation.EventSituation.{
  EventSituation,
  Insert,
  PreserveDates,
  UpdateSelf
}

import scala.concurrent.ExecutionContext

object EventSituation extends Enumeration {
  type EventSituation = Value
  val Insert, UpdateSelf, PreserveDates = Value
}

class ConservationService @Inject()(
    implicit
    val dao: ConservationDao,
    val actorRoleDateDao: ActorRoleDateDao,
    val subEventDao: TreatmentDao, //Arbitrary choice, to get access to helper functions irrespective of event type
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationService])

  def getEventTypeId(eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[EventTypeId]] = {
    dao.getEventTypeId(eventId)
  }

  def getRoleList: FutureMusitResult[Seq[EventRole]] = {
    actorRoleDateDao.getRoleList
  }

  def deleteSubEvents(
      mid: MuseumId,
      eventIds: Seq[EventId]
  ): FutureMusitResult[Unit] = {
    FutureMusitResult
      .collectAllOrFail[EventId, Unit](
        eventIds,
        eid => dao.deleteSubEvent(mid, eid).map(m => Some(m)),
        eventIds => MusitValidationError(s"Unable to delete eventIds:$eventIds")
      )
      .map(m => ())
  }

  def checkTypeOfObjects(objects: Seq[ObjectUUID]): FutureMusitResult[Unit] = {
    val res = objects.map(obj => dao.isValidObject(obj))
    FutureMusitResult.sequence(res).map(m => m.find(b => !b)).mapAndFlattenMusitResult {
      case Some(b) => {
        MusitValidationError("One or more objectIds are not in the object table")
      }
      case None => {
        MusitSuccess(())
      }
    }
  }

  private def findUpdatedInfo(
      mid: MuseumId,
      eventId: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ActorDate]] = {
    subEventDao.findUpdatedActorDate(mid, eventId)
  }

  private def findRegisteredInfo(
      mid: MuseumId,
      eventId: EventId
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[Option[ActorDate]] = {
    subEventDao.findRegisteredActorDate(mid, eventId)
  }

  def updateProcessWithDateAndActor(
      mid: MuseumId,
      event: ConservationProcess,
      situation: EventSituation,
      currentTimeAndActor: ActorDate
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[ConservationProcess] = {
    situation match {

      case Insert =>
        FutureMusitResult.from(event.withRegisteredInfoEx(currentTimeAndActor))
      case UpdateSelf => {
        val eventId = event.id.get
        findRegisteredInfo(mid, eventId)
          .getOrError(
            MusitValidationError(
              s"Unable to find conservation event registeredInfo: $eventId"
            )
          )
          .map(
            actorDate =>
              event.withRegisteredInfoEx(actorDate).withUpdatedInfoEx(currentTimeAndActor)
          )
      }

      case PreserveDates => {
        val eventId = event.id.get
        for {

          registeredInfo <- findRegisteredInfo(mid, eventId).getOrError(
                             MusitValidationError(
                               s"Unable to find conservation event registeredIinfo: $eventId"
                             )
                           )

          maybeUpdatedInfo <- findUpdatedInfo(mid, eventId)
        } yield {
          val regInfoEvent = event.withRegisteredInfoEx(registeredInfo)
          maybeUpdatedInfo.fold(regInfoEvent)(
            updatedInfo => regInfoEvent.withRegisteredInfoEx(updatedInfo)
          )
        }
      }
    }
  }

  def updateSubEventWithDateAndActor(
      mid: MuseumId,
      event: ConservationEvent,
      situation: EventSituation,
      currentTimeAndActor: ActorDate
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[ConservationEvent] = {
    require(situation != PreserveDates) // an illegal situation for subEvents
    situation match {

      case Insert =>
        FutureMusitResult.from(event.withRegisteredInfoEx(currentTimeAndActor))
      case UpdateSelf => {
        //update
        val eventId = event.id.get
        findRegisteredInfo(mid, eventId)
          .getOrError(
            MusitValidationError(
              s"Unable to find conservation event registeredIinfo: $eventId"
            )
          )
          .map(
            actorDate =>
              event.withRegisteredInfoEx(actorDate).withUpdatedInfoEx(currentTimeAndActor)
          )
      }

      case PreserveDates => {
        throw new IllegalArgumentException("impossible situation for subEvents")
      }
    }
  }

}
