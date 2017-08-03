package repositories.storage.dao

import com.google.inject.Inject
import models.storage.event.move.MoveObject
import no.uio.musit.MusitResults.{
  MusitDbError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID, StorageNodeId}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.SharedTables

import scala.concurrent.Future

class LocalObjectDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends SharedTables
    with DbErrorHandlers {

  val logger = Logger(classOf[LocalObjectDao])

  import profile.api._

  def asRow(mid: MuseumId, eventId: EventId, mo: MoveObject): Option[LocalObjectRow] = {
    mo.affectedThing.map { oid =>
      (oid, eventId, mo.to, mid, Some(mo.objectType.name))
    }
  }

  private def upsert(lo: LocalObjectRow): DBIO[MusitResult[Int]] =
    localObjectsTable.insertOrUpdate(lo).map {
      case num: Int if num == 1 || num == 0 =>
        MusitSuccess(num)

      case num: Int =>
        MusitDbError(s"Too many ($num)rows were updated")
    }

  def storeLatestMoveAction(
      mid: MuseumId,
      eventId: EventId,
      mo: MoveObject
  ): DBIO[MusitResult[Int]] = {
    asRow(mid, eventId, mo)
      .map(upsert)
      .getOrElse(
        DBIO.successful(
          MusitValidationError(
            "A MoveObject event requires both the " +
              "'affectedThing' and 'to' attributes set"
          )
        )
      )
  }

  def currentLocation(
      objectId: ObjectUUID,
      objectType: ObjectType
  ): Future[MusitResult[Option[StorageNodeId]]] = {
    val query = localObjectsTable.filter { locObj =>
      locObj.objectUuid === objectId &&
      (locObj.objectType === objectType.name || locObj.objectType.isEmpty)
    }.map(_.currentLocationId).max.result

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(s"An unexpected error occurred trying to get location for $objectId")
      )
  }

  def currentLocations(
      objectIds: Seq[ObjectUUID]
  ): Future[MusitResult[Map[ObjectUUID, Option[StorageNodeId]]]] = {
    type QLocQuery = Query[LocalObjectsTable, LocalObjectsTable#TableElementType, Seq]

    def buildQuery(ids: Seq[ObjectUUID]) =
      localObjectsTable.filter(_.objectUuid inSet ids)

    val q = objectIds.grouped(500).foldLeft[(Int, QLocQuery)]((0, localObjectsTable)) {
      case (qry, ids) =>
        if (qry._1 == 0) (1, buildQuery(ids))
        else (qry._1 + 1, qry._2 unionAll buildQuery(ids))
    }

    db.run(q._2.result)
      .map { l =>
        objectIds.foldLeft(Map.empty[ObjectUUID, Option[StorageNodeId]]) {
          case (res, oid) =>
            val maybeNodeId = l.find(_._1 == oid).map(_._3)
            res ++ Map(oid -> maybeNodeId)
        }
      }
      .map(MusitSuccess.apply)
      .recover(nonFatal("Unable to get current locations"))
  }
}
