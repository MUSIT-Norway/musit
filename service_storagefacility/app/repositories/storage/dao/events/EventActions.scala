package repositories.storage.dao.events

import models.storage.event.{EventTypeId, EventTypeRegistry, MusitEvent}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, MusitUUID, ObjectTypes}
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import repositories.shared.dao.DbErrorHandlers
import repositories.storage.dao.EventTables

import scala.concurrent.{ExecutionContext, Future}

/**
 * Common DB actions for all event types.
 */
trait EventActions extends DbErrorHandlers { self: EventTables =>

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
  )(implicit jsr: Reads[A]): Option[A#T] = {
    // The asInstanceOf can probably be made better with a better function
    // definition of the withId function
    Json.fromJson[A](row._11).asOpt.map(_.withId(row._1))
  }

  /** Action for inserting a new row in the event table */
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
    }.sortBy(_.eventId.desc)

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

    db.run(insertAction(row).transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred trying to add event ${e.eventType}"))
  }

  protected def insertBatch[A <: MusitEvent](
      mid: MuseumId,
      e: Seq[A]
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[EventId]]] = {
    val rows    = e.map(r => convertToRow(mid, r))
    val actions = DBIO.sequence(rows.map(r => insertAction(r)))

    db.run(actions.transactionally)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(
          "An exception occurred registering a batch move with ids: " +
            s" ${e.map(_.id.getOrElse("<empty>")).mkString(", ")}"
        )
      )
  }

  protected def insertBatchAnd[A <: MusitEvent, T](
      mid: MuseumId,
      e: Seq[A]
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A, EventId) => DBIO[T]
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[EventId]]] = {
    val actions = DBIO.sequence(e.map { r =>
      val row = convertToRow(mid, r)
      for {
        eid <- insertAction(row)
        _   <- additional(r, eid)
      } yield eid
    })

    db.run(actions.transactionally)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(
          "An exception occurred registering a batch move with ids: " +
            s" ${e.map(_.id.getOrElse("<empty>")).mkString(", ")}"
        )
      )
  }

  protected def findEventById[A <: MusitEvent](
      mid: MuseumId,
      id: EventId
  )(
      convertFromRow: EventRow => Option[A]
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[A]]] = {
    db.run(findByIdAction(mid, id))
      .map(res => MusitSuccess(res.flatMap(convertFromRow)))
      .recover(nonFatal(s"An error occurred trying to locate event $id"))
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
      .recover(
        nonFatal(
          s"An error occurred trying to locate events of type $eventTypeId for $id"
        )
      )
  }

}
