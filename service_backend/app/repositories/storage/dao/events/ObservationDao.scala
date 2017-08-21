package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents.ObservationEventType
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class ObservationDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends StorageEventTableProvider
    with EventActions
    with StorageFacilityEventRowMappers[Observation] {

  val logger = Logger(classOf[ObservationDao])

  /**
   * Writes a new Observation to the database table
   *
   * @param mid the MuseumId associated with the event
   * @param obs the Observation to save
   * @return the EventId given the event
   */
  def insert(
      mid: MuseumId,
      obs: Observation
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[EventId]] =
    insertEvent[Observation](mid, obs)(asRow)

  /**
   * Find the Observation with the given EventId
   *
   * @param mid the MuseumId associated with the event
   * @param id  the ID to lookup
   * @return the Observation that might be found
   */
  def findById(
      mid: MuseumId,
      id: EventId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[Observation]]] =
    findEventById[Observation](mid, id) { row =>
      fromRow(row._1, row._6, row._9.flatMap(StorageNodeId.fromString), row._12)
    }

  /**
   * List all Observation events for the given nodeId.
   *
   * @param mid    the MuseumId associated with the nodeId and Observation
   * @param nodeId the nodeId to find Observation for
   * @param limit  the number of results to return, defaults to all.
   * @return a list of Observation
   */
  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[Observation]]] =
    listEvents[Observation, StorageNodeId](
      mid,
      nodeId,
      ObservationEventType.id,
      limit
    ) { row =>
      fromRow(row._1, row._6, row._9.flatMap(StorageNodeId.fromString), row._12)
    }

}
