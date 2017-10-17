package services.conservation

import com.google.inject.Inject
import models.conservation.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{CollectionUUID, EventId, EventTypeId, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
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

  /**
   * Add a new ConservationEvent.
   */
  def add(mid: MuseumId, cpe: ConservationProcess)(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[ConservationModuleEvent]]] = {
    val res = for {
      added <- MusitResultT(addConservation(mid, cpe))
      a     <- MusitResultT(conservationDao.findConservationProcessById(mid, added))
    } yield a
    res.value
  }

  /**
   * Helper method specifically for adding an Analysis.
   */
  private def addConservation(
      mid: MuseumId,
      ce: ConservationProcess
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val res = for {
      eid <- MusitResultT(conservationDao.insert(mid, ce))
    } yield eid

    res.value
  }

  /**
   * Locate an event with the given EventId.
   */
  def findConservationProcessById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[ConservationModuleEvent]]] = {
    conservationDao.findConservationProcessById(mid, id)
  }

  def localupdateConservationProcess(
      cpFromDb: ConservationProcess,
      cme: ConservationModuleEvent
  )(implicit cu: AuthenticatedUser): ConservationProcess = {
    assert(cpFromDb.eventTypeId == cme.eventTypeId)
    assert(cpFromDb.id == cme.id || cme.id == None)

    cme match {
      case cp: ConservationProcess =>
        cpFromDb.copy(
          doneBy = cp.doneBy,
          doneDate = cp.doneDate,
          updatedBy = Some(cu.id),
          updatedDate = Some(dateTimeNow),
          completedBy = cp.completedBy,
          completedDate = cp.completedDate,
          note = cp.note,
          affectedThings = cp.affectedThings,
          caseNumber = cp.caseNumber,
          doneByActors = cp.doneByActors
        )
      case pres: Treatment            => ???
      case tech: TechnicalDescription => ???

    }
  }

  /**
   * Update an conservationProcess
   */
  def update(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationModuleEvent
  )(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Option[ConservationProcess]]] = {
    val res = for {
      maybeEvent <- MusitResultT(
                     conservationDao.findConservationProcessById(mid, eventId)
                   )
      maybeUpdated <- MusitResultT(
                       maybeEvent.map { e =>
                         val u = localupdateConservationProcess(e, cp)
                         conservationDao.update(mid, eventId, u)
                       }.getOrElse(Future.successful(MusitSuccess(None)))
                     )
    } yield maybeUpdated
    res.value
  }

}
