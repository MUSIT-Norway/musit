package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents.EnvRequirementEventType
import models.storage.event.envreq.EnvRequirement
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.EventTables

import scala.concurrent.Future

@Singleton
class EnvReqDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends EventTables
    with EventActions {

  val logger = Logger(classOf[EnvReqDao])

  /**
   * Writes a new EnvRequirement to the database table
   *
   * @param mid    the MuseumId associated with the event
   * @param envReq the EnvRequirement to save
   * @return the EventId given the event
   */
  def insert(
      mid: MuseumId,
      envReq: EnvRequirement
  ): Future[MusitResult[EventId]] =
    insertEvent[EnvRequirement](mid, envReq)(asRow[EnvRequirement])

  /**
   * Find the EnvRequirement with the given EventId
   *
   * @param mid the MuseumId associated with the event
   * @param id  the ID to lookup
   * @return the EnvRequirement that might be found
   */
  def findById(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[EnvRequirement]]] =
    findEventById[EnvRequirement](mid, id)(fromRow[EnvRequirement])

  /**
   * List all EnvRequirement events for the given nodeId.
   *
   * @param mid    the MuseumId associated with the nodeId and EnvRequirement
   * @param nodeId the nodeId to find EnvRequirement for
   * @param limit  the number of results to return, defaults to all.
   * @return a list of EnvRequirement
   */
  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[EnvRequirement]]] =
    listEvents[EnvRequirement, StorageNodeId](
      mid,
      nodeId,
      EnvRequirementEventType.id,
      limit
    )(fromRow[EnvRequirement])

  def latestForNodeId(
      mid: MuseumId,
      nodeId: StorageNodeId
  ): Future[MusitResult[Option[EnvRequirement]]] = {
    storageEventTable.filter { e =>
      e.eventTypeId === EnvRequirementEventType.id
    }
    ???
  }

}
