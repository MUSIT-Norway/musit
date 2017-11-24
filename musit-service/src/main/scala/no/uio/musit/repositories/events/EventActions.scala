package no.uio.musit.repositories.events

import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.repositories.{BaseColumnTypeMappers, DbErrorHandlers}
import no.uio.musit.security.AuthenticatedUser
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait EventRowMappers[A <: MusitEvent] { self: BaseEventTableProvider =>

  /**
   * Convert an instance of MusitEvent to an EventRow tuple
   */
  protected def asRow(
      mid: MuseumId,
      e: A
  )(implicit jsw: Writes[A], currUsr: AuthenticatedUser): EventRow

  /**
   * Convert an EventRow tuple to an instance of A
   */
  protected def fromRow(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      jsonColumn: JsValue
  )(implicit jsr: Reads[A]): Option[A] =
    Json.fromJson[A](jsonColumn).asOpt.map { row =>
      row
        .withId(maybeEventId)
        .withDoneDate(maybeDoneDate)
        .withAffectedThing(maybeAffectedThing)
        .asInstanceOf[A]
    }

}

/**
 * Common DB actions for all event types.
 */
trait EventActions extends DbErrorHandlers {
  self: BaseEventTableProvider
    with HasDatabaseConfigProvider[JdbcProfile]
    with EventRowMappers[_]
    with BaseColumnTypeMappers =>

  import profile.api._

  val logger: Logger

  protected val noaction: DBIO[Unit] = DBIO.successful(())

  /** Action for inserting a new row in the event table */
  protected def insertAction(event: EventRow): DBIO[EventId] = {
    eventTable returning eventTable.map(_.eventId) += event
  }

  /** Locate an event by the given ID */
  protected def findByIdAction(
      mid: MuseumId,
      id: EventId
  ): DBIO[Option[EventRow]] =
    eventTable.filter { e =>
      e.museumId === mid &&
      e.eventId === id
    }.result.headOption

  /** List all events for the affectedUuid */
  protected def listEventsAction[ID <: MusitUUID](
      mid: MuseumId,
      affectedId: ID,
      eventTypeId: EventTypeId,
      limit: Option[Int] = None
  ): DBIO[Seq[EventRow]] = {
    val q = eventTable.filter { e =>
      e.museumId === mid &&
      e.eventTypeId === eventTypeId &&
      e.affectedUuid === affectedId.asString
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
      .recover(nonFatal(s"An error occurred trying to add event ${e.getClass.getName}"))
  }

  protected def insertEventWithAdditionalAction[A <: MusitEvent, T](
      mid: MuseumId,
      e: A
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A, EventId) => DBIO[T]
  )(implicit ec: ExecutionContext): DBIO[EventId] = {
    val row = convertToRow(mid, e)
    for {
      eid <- insertAction(row)
      _   <- additional(e, eid)
    } yield eid
  }

  protected def insertEventWithAdditional[A <: MusitEvent, T](
      mid: MuseumId,
      e: A
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A, EventId) => DBIO[T]
  )(implicit ec: ExecutionContext): Future[MusitResult[EventId]] = {
    val action = insertEventWithAdditionalAction(mid, e)(convertToRow)(additional)

    db.run(action.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred trying to add event ${e.getClass.getName}"))
  }

  protected def insertAdditionalWithEventAction[A <: MusitEvent, T](
      mid: MuseumId,
      e: A
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A) => DBIO[T]
  )(implicit ec: ExecutionContext): DBIO[EventId] = {
    val row = convertToRow(mid, e)
    for {
      _   <- additional(e)
      eid <- insertAction(row)
    } yield eid
  }

  protected def insertAdditionalWithEvent[A <: MusitEvent, T](
      mid: MuseumId,
      e: A
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A) => DBIO[T]
  )(implicit ec: ExecutionContext): Future[MusitResult[EventId]] = {
    val action = insertAdditionalWithEventAction(mid, e)(convertToRow)(additional)

    db.run(action.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An error occurred trying to add event ${e.getClass.getName}"))
  }

  protected def insertBatchAction[A <: MusitEvent](
      mid: MuseumId,
      e: Seq[A]
  )(convertToRow: (MuseumId, A) => EventRow): DBIO[Seq[EventId]] = {
    val rows = e.map(r => convertToRow(mid, r))
    DBIO.sequence(rows.map(r => insertAction(r)))
  }

  protected def insertBatch[A <: MusitEvent](
      mid: MuseumId,
      e: Seq[A]
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[EventId]]] = {
    val actions = insertBatchAction(mid, e)(convertToRow)

    db.run(actions.transactionally)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(
          "An exception occurred registering a batch move with ids: " +
            s" ${e.map(_.id.getOrElse("<empty>")).mkString(", ")}"
        )
      )
  }

  protected def insertBatchWithAdditionalAction[A <: MusitEvent, T](
      mid: MuseumId,
      e: Seq[A]
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A, EventId) => DBIO[T]
  )(implicit ec: ExecutionContext): DBIO[Seq[EventId]] = {
    DBIO.sequence(e.map { r =>
      val row = convertToRow(mid, r)
      for {
        eid <- insertAction(row)
        _   <- additional(r, eid)
      } yield eid
    })
  }

  protected def insertBatchWithAdditional[A <: MusitEvent, T](
      mid: MuseumId,
      e: Seq[A]
  )(
      convertToRow: (MuseumId, A) => EventRow
  )(
      additional: (A, EventId) => DBIO[T]
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[EventId]]] = {
    val actions = insertBatchWithAdditionalAction(mid, e)(convertToRow)(additional)

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
      .map(res => MusitSuccess(res.flatMap(r => convertFromRow(r))))
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
        MusitSuccess(res.flatMap(r => convertFromRow(r)))
      }
      .recover(
        nonFatal(
          s"An error occurred trying to locate events of type $eventTypeId for $id"
        )
      )
  }

}
