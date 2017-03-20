package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents
import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.move.{MoveEvent, MoveNode, MoveObject}
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.EventTables

import scala.concurrent.Future

@Singleton
class MoveDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends EventTables
    with EventActions {

  val logger = Logger(classOf[MoveDao])

  /**
   * Writes a new MoveEvent to the database table
   *
   * @param mid  the MuseumId associated with the event
   * @param moveEvent the MoveEvent to save
   * @return the EventId given the event
   */
  def insert[A <: MoveEvent](
      mid: MuseumId,
      moveEvent: A
  ): Future[MusitResult[EventId]] =
    moveEvent match {
      case mn: MoveNode   => insertEvent[MoveNode](mid, mn)(asRow)
      case mo: MoveObject => insertEvent[MoveObject](mid, mo)(asRow)
    }

  /**
   * Find the MoveEvent with the given EventId
   *
   * @param mid the MuseumId associated with the event
   * @param id the ID to lookup
   * @return the MoveEvent that might be found
   */
  def findById(
      mid: MuseumId,
      id: EventId
  ): Future[MusitResult[Option[MoveEvent]]] =
    findEventById[MoveEvent](mid, id) { row =>
      TopLevelEvents.unsafeFromId(row._2) match {
        case MoveNodeType   => fromRow[MoveNode](row)
        case MoveObjectType => fromRow[MoveObject](row)
        case _              => None
      }
    }

  /**
   * List all MoveNode events for the given nodeId.
   *
   * @param mid the MuseumId associated with the nodeId and MoveNode
   * @param nodeId the nodeId to find MoveNode for
   * @param limit the number of results to return, defaults to all.
   * @return a list of MoveNode
   */
  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[MoveNode]]] =
    listEvents[MoveNode, StorageNodeId](
      mid,
      nodeId,
      MoveNodeType.id,
      limit
    )(fromRow)

  /**
   * List all MoveObject events for the given objectUUID.
   *
   * @param mid the MuseumId associated with the objectUUID and MoveObject
   * @param objectUUID the nodeId to find MoveNode for
   * @param limit the number of results to return, defaults to all.
   * @return a list of MoveObject
   */
  def list(
      mid: MuseumId,
      objectUUID: ObjectUUID,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[MoveObject]]] =
    listEvents[MoveObject, ObjectUUID](
      mid,
      objectUUID,
      MoveObjectType.id,
      limit
    )(fromRow)

}
