package repositories.storage.old_dao

import com.google.inject.Inject
import models.storage.MovableObject_Old
import models.storage.event.dto.{EventDto, LocalObject}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{EventId, MuseumId, ObjectId, StorageNodeDatabaseId}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

// TODO: Remove me once data is migrated from old to new structure
class LocalObjectDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  private def upsert(lo: LocalObject): DBIO[Int] =
    localObjectsTable.insertOrUpdate(lo)

  def storeLatestMove(mid: MuseumId, eventId: EventId, moveEvent: EventDto): DBIO[Int] = {
    val relObj = moveEvent.relatedObjects.headOption
    val relPlc = moveEvent.relatedPlaces.headOption
    val objTpe = moveEvent.valueString.getOrElse(CollectionObject.name)

    relObj.flatMap { obj =>
      relPlc.map { place =>
        upsert(
          LocalObject(obj.objectId, eventId, place.placeId, mid, objTpe)
        )
      }
    }.getOrElse(
      throw new AssertionError(
        "A MoveObject event requires both the " +
          "'affectedThing' and 'to' attributes set"
      )
    )
  }

  def currentLocation(
      objectId: ObjectId,
      objectType: ObjectType
  ): Future[Option[StorageNodeDatabaseId]] = {
    val query = localObjectsTable.filter { locObj =>
      locObj.objectId === objectId &&
      (locObj.objectType === objectType.name || locObj.objectType.isEmpty)
    }.map(_.currentLocationId).max.result

    db.run(query)
  }

  /**
   * Returns the LocalObject instance associated with the given objectIds
   *
   * @param objectIds Seq of ObjectIds to get current location for.
   * @return Eventually returns a Map of ObjectIds and StorageNodeDatabaseId
   */
  def currentLocations(
      objectIds: Seq[ObjectId]
  ): Future[MusitResult[Map[ObjectId, Option[StorageNodeDatabaseId]]]] = {
    type QLocQuery = Query[LocalObjectsTable, LocalObjectsTable#TableElementType, Seq]

    def buildQuery(ids: Seq[ObjectId]) = localObjectsTable.filter(_.objectId inSet ids)

    val q = objectIds.grouped(500).foldLeft[(Int, QLocQuery)]((0, localObjectsTable)) {
      case (qry, ids) =>
        if (qry._1 == 0) (1, buildQuery(ids))
        else (qry._1 + 1, qry._2 unionAll buildQuery(ids))
    }

    db.run(q._2.result)
      .map { l =>
        objectIds.foldLeft(Map.empty[ObjectId, Option[StorageNodeDatabaseId]]) {
          case (res, oid) =>
            val maybeNodeId = l.find(_.objectId == oid).map(_.currentLocationId)
            res ++ Map(oid -> maybeNodeId)
        }
      }
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(ex) =>
          MusitDbError("Unable to get current location", Some(ex))
      }

  }

  /**
   * Returns the LocalObject instance associated with the given objectIds
   *
   * @param movableObjs Seq of MobableObjects to get current location for.
   * @return Eventually returns a Map of ObjectIds and StorageNodeDatabaseId
   */
  def currentLocationsForMovableObjects(
      movableObjs: Seq[MovableObject_Old]
  ): Future[MusitResult[Map[MovableObject_Old, Option[StorageNodeDatabaseId]]]] = {
    type QLocQuery = Query[LocalObjectsTable, LocalObjectsTable#TableElementType, Seq]

    val typById = movableObjs.groupBy(_.objectType).mapValues(_.map(_.id))

    def buildSingleQuery(tpy: ObjectType, ids: Seq[ObjectId]) =
      localObjectsTable.filter(
        loc => loc.objectType === tpy.name && (loc.objectId inSet ids)
      )

    def buildGroupedQuery(tpy: ObjectType, oids: Seq[ObjectId]) =
      oids.grouped(500).foldLeft[Option[QLocQuery]](None) {
        case (qry, ids) =>
          qry match {
            case None  => Some(buildSingleQuery(tpy, ids))
            case other => other.map(_ unionAll buildSingleQuery(tpy, ids))
          }
      }

    val query = typById.foldLeft[Option[QLocQuery]](None) {
      case (qry, (typ, ids)) =>
        qry match {
          case None           => buildGroupedQuery(typ, ids)
          case Some(otherQry) => buildGroupedQuery(typ, ids).map(otherQry unionAll)
        }
    }

    query.map { qry =>
      db.run(qry.result)
        .map { l =>
          movableObjs.foldLeft(
            Map.empty[MovableObject_Old, Option[StorageNodeDatabaseId]]
          ) {
            case (res, oid) =>
              val maybeNodeId = l.find { res =>
                res.objectId == oid.id && res.objectType == oid.objectType.name
              }.map(_.currentLocationId)
              res ++ Map(oid -> maybeNodeId)
          }
        }
        .map(MusitSuccess.apply)
        .recover {
          case NonFatal(ex) =>
            MusitDbError("Unable to get current location", Some(ex))
        }
    }.getOrElse(Future.successful(MusitSuccess(Map.empty)))
  }

  val localObjectsTable = TableQuery[LocalObjectsTable]

  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObject](tag, SchemaName, "LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * =
      (
        objectId,
        latestMoveId,
        currentLocationId,
        museumId,
        objectType
      ) <> (create.tupled, destroy)

    // scalastyle:on method.name

    val objectId          = column[ObjectId]("OBJECT_ID", O.PrimaryKey)
    val latestMoveId      = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeDatabaseId]("CURRENT_LOCATION_ID")
    val museumId          = column[MuseumId]("MUSEUM_ID")
    val objectType        = column[Option[String]]("OBJECT_TYPE")

    def create =
      (
          objectId: ObjectId,
          latestMoveId: EventId,
          currentLocationId: StorageNodeDatabaseId,
          museumId: MuseumId,
          objectType: Option[String]
      ) =>
        LocalObject(
          objectId = objectId,
          latestMoveId = latestMoveId,
          currentLocationId = currentLocationId,
          museumId = museumId,
          objectType = objectType.getOrElse(CollectionObject.name)
      )

    def destroy(localObject: LocalObject) =
      Some(
        (
          localObject.objectId,
          localObject.latestMoveId,
          localObject.currentLocationId,
          localObject.museumId,
          Option(localObject.objectType)
        )
      )
  }

}
