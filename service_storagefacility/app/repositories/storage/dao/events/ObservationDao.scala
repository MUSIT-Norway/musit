package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents.ObservationEventType
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.EventTables

import scala.concurrent.Future

@Singleton
class ObservationDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends EventTables
    with EventActions {

  val logger = Logger(classOf[ObservationDao])

  /**
   * Writes a new Observation to the database table
   *
   * @param mid  the MuseumId associated with the event
   * @param obs the Observation to save
   * @return the EventId given the event
   */
  def insert(
      mid: MuseumId,
      obs: Observation
  ): Future[MusitResult[EventId]] =
    insertEvent[Observation](mid, obs)(asRow)

  /**
   * Find the Observation with the given EventId
   *
   * @param mid the MuseumId associated with the event
   * @param id the ID to lookup
   * @return the Observation that might be found
   */
  def findById(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[Observation]]] =
    findEventById[Observation](mid, id)(fromRow)

  /**
   * List all Observation events for the given nodeId.
   *
   * @param mid the MuseumId associated with the nodeId and Observation
   * @param nodeId the nodeId to find Observation for
   * @param limit the number of results to return, defaults to all.
   * @return a list of Observation
   */
  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[Observation]]] =
    listEvents[Observation, StorageNodeId](
      mid,
      nodeId,
      ObservationEventType.id,
      limit
    )(fromRow)

}
