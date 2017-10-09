package services.conservation

import com.google.inject.Inject
import models.conservation.events.{ConservationEvent, ConservationModuleEvent}
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{EventId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.ConservationEventDao

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class ConservationEventService[T <: ConservationEvent: ClassTag] @Inject()(
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
  ): Future[MusitResult[Option[ConservationEvent]]] = {
    val res = for {
      added <- MusitResultT(dao.insert(mid, ce))
      a     <- MusitResultT(dao.findSpecificById(mid, added))
    } yield a
    res.value
  }

  /**
   * Helper method specifically for adding an Analysis.
   ***/
  private def addConservationEvent(
      mid: MuseumId,
      ce: T
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val res = for {
      eid <- MusitResultT(dao.insert(mid, ce))
    } yield eid

    res.value
  }

  /**
   * Locate an event with the given EventId.
   */
  def findConservationEventById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[T]]] = {
    dao.findSpecificById(mid, id)
  }

  //  def localupdateConservationProcess(
  //                                      cpFromDb: ConservationProcess,
  //                                      cme: ConservationModuleEvent
  //                                    )(implicit cu: AuthenticatedUser): ConservationProcess = {
  //    assert(cpFromDb.eventTypeId == cme.eventTypeId)
  //    assert(cpFromDb.id == cme.id || cme.id == None)
  //
  //    cme match {
  //      case cp: ConservationProcess =>
  //        cpFromDb.copy(
  //          doneBy = cp.doneBy,
  //          doneDate = cp.doneDate,
  //          updatedBy = Some(cu.id),
  //          updatedDate = Some(dateTimeNow),
  //          completedBy = cp.completedBy,
  //          completedDate = cp.completedDate,
  //          note = cp.note,
  //          affectedThings = cp.affectedThings,
  //          caseNumber = cp.caseNumber,
  //          doneByActors = cp.doneByActors
  //        )
  //      case pres: Preservation => ???
  //      case prep: Preparation  => ???
  //
  //    }
  //  }
  //
  //  /**
  //    * Update an conservationProcess
  //    */
  //  def update(
  //              mid: MuseumId,
  //              eventId: EventId,
  //              cp: ConservationModuleEvent
  //            )(
  //              implicit currUser: AuthenticatedUser
  //            ): Future[MusitResult[Option[ConservationProcess]]] = {
  //    val res = for {
  //      maybeEvent <- MusitResultT(
  //        conservationDao.findConservationProcessById(mid, eventId)
  //      )
  //      maybeUpdated <- MusitResultT(
  //        maybeEvent.map { e =>
  //          val u = localupdateConservationProcess(e, cp)
  //          conservationDao.update(mid, eventId, u)
  //        }.getOrElse(Future.successful(MusitSuccess(None)))
  //      )
  //    } yield maybeUpdated
  //    res.value
  //  }

}
