package repositories.storage.dao.events

import models.storage.event.EventTypeRegistry.TopLevelEvent
import no.uio.musit.models.{EventId, MuseumId, StorageNodeId}
import repositories.storage.dao.EventTables

/**
 * Common DB actions for all event types.
 */
trait EventActions { self: EventTables =>

  import profile.api._

  protected val noaction: DBIO[Unit] = DBIO.successful(())

  /** Action for inserting a new row in the evnet table */
  protected def insertAction(event: EventRow): DBIO[EventId] =
    storageEventTable returning storageEventTable.map(_.eventId) += event

  /** Locate an event by the given ID */
  protected def findByIdAction(id: EventId): DBIO[Option[EventRow]] =
    storageEventTable.filter(_.eventId === id).result.headOption

  /** List all events for the given StorageNodeId */
  protected def list[EType <: TopLevelEvent](
      mid: MuseumId,
      nodeId: StorageNodeId,
      eventType: EType,
      limit: Option[Int] = None
  ): DBIO[Seq[EventRow]] = {
    val q = storageEventTable.filter { e =>
      (e.museumId.isEmpty || e.museumId === mid) &&
      e.eventTypeId === eventType.id &&
      e.affectedUuid === nodeId.asString
    }.sortBy(_.registeredDate.desc)

    limit.map {
      case lim: Int if lim == -1 => q
      case lim: Int if lim > 0   => q.take(lim)
      case lim: Int              => q.take(50)
    }.getOrElse(q).result
  }
}
