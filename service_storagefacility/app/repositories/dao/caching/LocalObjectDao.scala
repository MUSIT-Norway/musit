/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package repositories.dao.caching

import com.google.inject.Inject
import models.MovableObject
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import models.event.dto.{EventDto, LocalObject}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, ObjectId, StorageNodeDatabaseId}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.SharedTables

import scala.concurrent.Future
import scala.util.control.NonFatal

class LocalObjectDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends SharedTables {

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
      movableObjs: Seq[MovableObject]
  ): Future[MusitResult[Map[MovableObject, Option[StorageNodeDatabaseId]]]] = {
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
          movableObjs.foldLeft(Map.empty[MovableObject, Option[StorageNodeDatabaseId]]) {
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

}
