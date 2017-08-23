package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import models.storage.event.control.Control
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ControlDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends StorageEventTableProvider
    with EventActions
    with StorageFacilityEventRowMappers[Control] {

  override val logger = Logger(classOf[ControlDao])

  /**
   * Writes a new Control to the database table
   *
   * @param mid  the MuseumId associated with the event
   * @param ctrl the Control to save
   * @return the EventId given the event
   */
  def insert(
      mid: MuseumId,
      ctrl: Control
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] =
    insertEvent[Control](mid, ctrl)(asRow)

  /**
   * Find the Control with the given EventId
   *
   * @param mid the MuseumId associated with the event
   * @param id  the ID to lookup
   * @return the Control that might be found
   */
  def findById(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Control]]] =
    findEventById[Control](mid, id) { row =>
      fromRow(row._1, row._6, row._9.flatMap(StorageNodeId.fromString), row._12)
    }

  /**
   * List all Control events for the given nodeId.
   *
   * @param mid    the MuseumId associated with the nodeId and Controls
   * @param nodeId the nodeId to find Controls for
   * @param limit  the number of results to return, defaults to all.
   * @return a list of Controls
   */
  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[Control]]] =
    listEvents[Control, StorageNodeId](
      mid,
      nodeId,
      ControlEventType.id,
      limit
    ) { row =>
      fromRow(row._1, row._6, row._9.flatMap(StorageNodeId.fromString), row._12)
    }

}
