package repositories.storage.dao.events

import models.storage.event.{EventTypeId, EventTypeRegistry, MusitEvent}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, MusitUUID, ObjectTypes}
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import repositories.storage.dao.EventTables

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Common DB actions for all event types.
 */
trait EventActions { self: EventTables =>

  import profile.api._

  val logger: Logger

  protected val noaction: DBIO[Unit] = DBIO.successful(())

  protected def asRow[A <: MusitEvent](
      mid: MuseumId,
      e: A
  )(implicit jsw: Writes[A]): EventRow =
    (
      None,
      e.eventType.registeredEventId,
      Option(mid),
      Option(e.doneDate),
      e.registeredBy,
      e.registeredDate,
      None,
      e.affectedThing.map(_.asString),
      Some(ObjectTypes.Node),
      None,
      Json.toJson[A](e)
    )

  protected def fromRow[A <: MusitEvent](
      row: EventRow
  )(implicit jsr: Reads[A]): Option[A] = Json.fromJson[A](row._11).asOpt

  /** Action for inserting a new row in the evnet table */
  protected def insertAction(event: EventRow): DBIO[EventId] =
    storageEventTable returning storageEventTable.map(_.eventId) += event

  /** Locate an event by the given ID */
  protected def findByIdAction(
      mid: MuseumId,
      id: EventId
  ): DBIO[Option[EventRow]] =
    storageEventTable.filter { e =>
      e.eventId === id &&
      (e.museumId === mid || e.museumId.isEmpty)
    }.result.headOption

  /** List all events for the given StorageNodeId */
  protected def listEventsAction[ID <: MusitUUID](
      mid: MuseumId,
      nodeId: ID,
      eventTypeId: EventTypeId,
      limit: Option[Int] = None
  ): DBIO[Seq[EventRow]] = {
    val q = storageEventTable.filter { e =>
      (e.museumId.isEmpty || e.museumId === mid) &&
      e.eventTypeId === eventTypeId &&
      e.affectedUuid === nodeId.asString
    }.sortBy(_.registeredDate.desc)

    limit.map {
      case lim: Int if lim == -1 => q
      case lim: Int if lim > 0   => q.take(lim)
      case lim: Int              => q.take(50)
    }.getOrElse(q).result
  }

  protected def insertEvent[A <: MusitEvent](
      mid: MuseumId,
      e: A
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(implicit ec: ExecutionContext): Future[MusitResult[EventId]] = {
    val row = convertToRow(mid, e)

    db.run(insertAction(row)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = ""
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  protected def findEventById[A <: MusitEvent](
      mid: MuseumId,
      id: EventId
  )(
      convertFromRow: EventRow => Option[A]
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[A]]] = {
    db.run(findByIdAction(mid, id))
      .map(res => MusitSuccess(res.flatMap(convertFromRow)))
      .recover {
        case NonFatal(ex) =>
          val msg = ""
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))

      }
  }

  protected def listEvents[A <: MusitEvent, ID <: MusitUUID](
      mid: MuseumId,
      id: ID,
      eventTypeId: EventTypeId,
      limit: Option[Int] = None
  )(
      convertFromRow: EventRow => Option[A]
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[A]]] = {
    val q = listEventsAction(mid, id, eventTypeId, limit)

    db.run(q)
      .map { res =>
        logger.debug(
          s"Found ${res.size} rows of ${EventTypeRegistry.unsafeFromId(eventTypeId)}"
        )
        MusitSuccess(res.flatMap(r => convertFromRow(r)))
      }
      .recover {
        case NonFatal(ex) =>
          val msg = ""
          logger.error(msg, ex)
          MusitDbError(msg, Option(ex))
      }
  }

}
