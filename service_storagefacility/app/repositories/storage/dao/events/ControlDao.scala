package repositories.storage.dao.events

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import models.storage.event.control.Control
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, ObjectTypes, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import repositories.storage.dao.EventTables

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class ControlDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends EventTables
    with EventActions {

  def asRow(mid: MuseumId, ctrl: Control): EventRow = {
    (
      None,
      ctrl.eventType.registeredEventId,
      Option(mid),
      Option(ctrl.doneDate),
      ctrl.registeredBy,
      ctrl.registeredDate,
      None,
      None, // ctrl.affectedThing <- refactor to StorageNodeId which is an UUID
      Some(ObjectTypes.Node),
      None,
      Json.toJson(ctrl)
    )
  }

  def fromRow(row: EventRow): Option[Control] = {
    Json.fromJson(row._11).asOpt
  }

  val logger = Logger(classOf[ControlDao])

  def insert(mid: MuseumId, ctrl: Control): Future[MusitResult[EventId]] = {
    val row = asRow(mid, ctrl)

    db.run(insertAction(row)).map(MusitSuccess.apply).recover {
      case NonFatal(ex) =>
        val msg = ""
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

  def findById(id: EventId): Future[MusitResult[Option[Control]]] = {
    db.run(findByIdAction(id)).map(res => MusitSuccess(res.flatMap(fromRow))).recover {
      case NonFatal(ex) =>
        val msg = ""
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))

    }
  }

  def list(
      mid: MuseumId,
      nodeId: StorageNodeId,
      limit: Option[Int] = None
  ): Future[MusitResult[Seq[Control]]] = {
    val q = list(mid, nodeId, ControlEventType, limit)

    db.run(q).map(res => MusitSuccess(res.flatMap(fromRow))).recover {
      case NonFatal(ex) =>
        val msg = ""
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))

    }
  }

}
