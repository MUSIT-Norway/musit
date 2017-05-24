package services.storage

import com.google.inject.Inject
import models.storage.event.control.Control
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.events.ControlDao

import scala.concurrent.Future

class ControlService @Inject()(
    val controlDao: ControlDao,
    val nodeService: StorageNodeService
) {

  val logger = Logger(classOf[ControlService])

  def add(
      mid: MuseumId,
      nodeId: StorageNodeId,
      ctrl: Control
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Control]] = {
    nodeService.exists(mid, nodeId).flatMap {
      case MusitSuccess(nodeExists) =>
        if (nodeExists) {
          val c = ctrl.copy(
            affectedThing = Some(nodeId),
            registeredBy = Some(currUsr.id),
            registeredDate = Some(dateTimeNow)
          )
          (for {
            eid        <- MusitResultT(controlDao.insert(mid, c))
            maybeAdded <- MusitResultT(controlDao.findById(mid, eid))
            added <- MusitResultT
                      .successful(maybeAdded.map(MusitSuccess.apply).getOrElse {
                        logger.error(
                          s"An unexpected error occurred when trying to fetch a " +
                            s"control event that was added with eventId $eid"
                        )
                        MusitInternalError("Could not locate the control that was added")
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

  def findBy(mid: MuseumId, id: EventId): Future[MusitResult[Option[Control]]] = {
    controlDao.findById(mid, id)
  }

  def listFor(
      mid: MuseumId,
      nodeId: StorageNodeId
  ): Future[MusitResult[Seq[Control]]] = {
    controlDao.list(mid, nodeId)
  }
}
