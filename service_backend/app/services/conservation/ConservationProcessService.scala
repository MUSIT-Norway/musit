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

  /**
   * Add a new ConservationEvent.
   */
  def add(mid: MuseumId, cpe: ConservationProcess)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    val event = cpe.withRegisteredInfo(Some(currUser.id), Some(dateTimeNow))
    val res = for {
      added <- addConservation(mid, event)
      a     <- conservationDao.findConservationProcessById(mid, added)
    } yield a
    res
  }

  /**
   * Helper method specifically for adding an Analysis.
   */
  private def addConservation(
      mid: MuseumId,
      ce: ConservationProcess
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {

    conservationDao.insert(mid, ce)

  }

  /*
  private def addConservation2(
                               mid: MuseumId,
                               ce: ConservationProcess
                             )(implicit currUser: AuthenticatedUser): Future[MusitResult[EventId]] = {
    val res = for {
      eid <- MusitResultT(conservationDao.insert(mid, ce))
    } yield eid

    res.value
  }
   */
  /**
   * Locate an event with the given EventId.
   */
  def findConservationProcessById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {
    conservationDao.findConservationProcessById(mid, id)
  }

  /*def localupdateConservationProcess(
      cpFromDb: ConservationProcess,
      cme: ConservationModuleEvent
  )(implicit cu: AuthenticatedUser): ConservationProcess = {
    assert(cpFromDb.eventTypeId == cme.eventTypeId)
    assert(cpFromDb.id == cme.id || cme.id == None)
    cme match {
      case cp: ConservationProcess =>
        cpFromDb.copy(
          registeredBy = cp.registeredBy,
          registeredDate = cp.registeredDate
           doneBy = cp.doneBy,
          doneDate = cp.doneDate,
          updatedBy = Some(cu.id),
          updatedDate = Some(dateTimeNow),
          completedBy = cp.comp letedBy,
          completedDate = cp.completedDate,
          note = cp.note,
          affectedThings = cp.affectedThings,
          caseNumber = cp.caseNumber,
          doneByActors = cp.doneByActors
        )
      case pres: Treatment            => ???
      case tech: TechnicalDescription => ???

    }
  }*/

  def check(test: Boolean, errorMsg: String): MusitResult[Boolean] = {
    if (test) {
      MusitSuccess(true)
    } else {
      MusitValidationError(errorMsg)
    }
  }

  /*def finRegisteredByAndDate(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationProcess
  )(
      implicit currUser: AuthenticatedUser
  ): ConservationProcess = {
    val regby         = cp.registeredBy
    val futOptDbEvent = conservationDao.findConservationProcessById(mid, eventId)
    val res = futOptDbEvent.map { optdbEvent =>
      optdbEvent.map { maybedbEvent =>
        maybedbEvent.map { dbEvent =>
          val cpCopy = cp.copy(
            registeredBy = dbEvent.registeredBy,
            registeredDate = dbEvent.registeredDate
          )
          cpCopy
        }.getOrElse(None)
      }.getOrElse(None)
    }
    val nres = res.asInstanceOf[ConservationProcess]
    println("etter copyfunksjonen  ")
    nres

  }*/

  import MusitResultUtils._

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

    def getRegisteredActorDate(
        futMrEvent: FutureMusitResult[ConservationModuleEvent]
    ): FutureMusitResult[ActorDate] = {
      futMrEvent.map(
        event => ActorDate(event.registeredBy.get, event.registeredDate.get)
      )
    }

    def findRegisteredActorDate(localEventId: EventId): FutureMusitResult[ActorDate] = {
      getRegisteredActorDate(
        conservationDao
          .findConservationModuleById(mid, localEventId)
          .getOrError(
            MusitValidationError(
              s"Unable to find conservation subevent with id: $eventId"
            )
          )
      )
    }

    check(
      Some(eventId) == cp.id,
      s"Inconsistent eventid in url($eventId) vs body (${cp.id})"
    ).flatMapToFutureMusitResult { _ =>
      val res = for {
        eventToWriteToDb <- putUpdateAndRegDataToProcessAndSubevents(
                             cp,
                             currUser.id,
                             dateTimeNow,
                             findRegisteredActorDate
                           )

        maybeUpdated <- conservationDao.update(mid, eventId, eventToWriteToDb)

      } yield maybeUpdated
      res
    }

  }

  /*check(
      Some(eventId) == cp.id,
      s"Inconsistent eventid in url($eventId) vs body (${cp.id})"
    ).flatMapToFutureMusitResult { _ =>
      val eventToWriteToDb = cp.withUpdatedInfo(Some(currUser.id), Some(dateTimeNow))
      val eventWithRegData = finRegisteredByAndDate(mid, eventId, eventToWriteToDb)
      println("etter copy  " + eventWithRegData)
      val updateRes = conservationDao.update(mid, eventId, eventWithRegData)
      updateRes
    }

  }*/
  /*val res = for {
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
  }*/

}
