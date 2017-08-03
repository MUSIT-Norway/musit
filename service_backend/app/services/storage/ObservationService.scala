package services.storage

import com.google.inject.Inject
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.events.ObservationDao

import scala.concurrent.Future

class ObservationService @Inject()(
    val observationDao: ObservationDao,
    val nodeService: StorageNodeService
) {

  val logger = Logger(classOf[ObservationService])

  def add(
      mid: MuseumId,
      nodeId: StorageNodeId,
      obs: Observation
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Observation]] = {
    nodeService.exists(mid, nodeId).flatMap {
      case MusitSuccess(nodeExists) =>
        if (nodeExists) {
          val o = obs.copy(
            affectedThing = Some(nodeId),
            registeredBy = Some(currUsr.id),
            registeredDate = Some(dateTimeNow)
          )
          (for {
            eid        <- MusitResultT(observationDao.insert(mid, o))
            maybeAdded <- MusitResultT(observationDao.findById(mid, eid))
            added <- MusitResultT
                      .successful(maybeAdded.map(MusitSuccess.apply).getOrElse {
                        logger.error(
                          s"An unexpected error occurred when trying to fetch an " +
                            s"observation event that was added with eventId $eid"
                        )
                        MusitInternalError(
                          "Could not locate the observation that was added"
                        )
                      })
          } yield added).value

        } else {
          Future.successful(MusitValidationError("Node not found."))
        }

      case err: MusitError =>
        logger.error("An error occured when trying to add a Control")
        Future.successful(err)
    }
  }

  def findBy(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Observation]]] = {
    observationDao.findById(mid, id)
  }

  def listFor(
      mid: MuseumId,
      nodeId: StorageNodeId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[Observation]]] = {
    observationDao.list(mid, nodeId)
  }

}
