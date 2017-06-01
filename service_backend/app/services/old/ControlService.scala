package services.old

import com.google.inject.Inject
import models.storage.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import models.storage.event.dto.BaseEventDto
import models.storage.event.dto.DtoConverters.CtrlConverters
import models.storage.event.old.control.Control
import no.uio.musit.MusitResults._
import no.uio.musit.models.{EventId, MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.event.EventDao

import scala.concurrent.Future

class ControlService @Inject()(
    val eventDao: EventDao,
    val storageNodeService: StorageNodeService
) {

  val logger = Logger(classOf[ControlService])

  /**
   *
   * @param mid
   * @param nodeId
   * @param ctrl
   * @param currUsr
   * @return
   */
  def add(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId,
      ctrl: Control
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Control]] = {
    storageNodeService.exists(mid, nodeId).flatMap {
      case MusitSuccess(nodeExists) =>
        if (nodeExists) {
          val c = ctrl.copy(
            affectedThing = Some(nodeId),
            registeredBy = Some(currUsr.id),
            registeredDate = Some(dateTimeNow)
          )
          val dto = CtrlConverters.controlToDto(c)
          eventDao.insertEvent(mid, dto).flatMap { eventId =>
            eventDao.getEvent(mid, eventId).map { res =>
              res.flatMap(_.map { dto =>
                // We know we have a BaseEventDto representing a Control.
                val bdto = dto.asInstanceOf[BaseEventDto]
                MusitSuccess(CtrlConverters.controlFromDto(bdto))
              }.getOrElse {
                logger.error(
                  s"An unexpected error occured when trying to fetch a " +
                    s"control event that was added with eventId $eventId"
                )
                MusitInternalError("Could not locate the control that was added")
              })
            }
          }
        } else {
          Future.successful(MusitValidationError("Node not found."))
        }

      case err: MusitError =>
        logger.error("An error occured when trying to add a Control")
        Future.successful(err)
    }
  }

  /**
   *
   * @param id
   * @return
   */
  def findBy(mid: MuseumId, id: EventId): Future[MusitResult[Option[Control]]] = {
    eventDao.getEvent(mid, id).map { result =>
      result.flatMap(_.map {
        case base: BaseEventDto =>
          MusitSuccess(
            Option(CtrlConverters.controlFromDto(base))
          )

        case _ =>
          MusitInternalError(
            "Unexpected DTO type. Expected BaseEventDto with event type Control"
          )
      }.getOrElse(MusitSuccess(None)))
    }
  }

  def listFor(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Seq[Control]]] = {
    eventDao.getEventsForNode(mid, nodeId, ControlEventType).map { dtos =>
      MusitSuccess(dtos.map { dto =>
        // We know we have a BaseEventDto representing a Control.
        val base = dto.asInstanceOf[BaseEventDto]
        CtrlConverters.controlFromDto(base)
      })
    }
  }

}
