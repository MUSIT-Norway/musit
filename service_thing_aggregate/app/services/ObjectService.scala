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

package services

import com.google.inject.Inject
import models.{MusitObject, ObjectSearchResult}
import no.uio.musit.MusitResults._
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.{ObjectDao, StorageNodeDao}

import scala.concurrent.Future
import scala.util.control.NonFatal

class ObjectService @Inject() (
    val objDao: ObjectDao,
    val nodeDao: StorageNodeDao
) {

  private val logger = Logger(classOf[ObjectService])

  /**
   * Service that looks up objects using the old primary key in for the old DB
   * schema name. Implementation is specific to the Delphi client integration.
   *
   * @param oldSchema    The old DB schema name
   * @param oldObjectIds The local primary key for the given schema name.
   * @return A list containing the _new_ ObjectIds for the objects.
   */
  def findByOldObjectIds(
    oldSchema: String,
    oldObjectIds: Seq[Long]
  ): Future[MusitResult[Seq[ObjectId]]] = {
    objDao.findObjectIdsForOld(oldSchema, oldObjectIds)
  }

  /**
   * A helper method for getting the current location of an object
   *
   * @param mid         The MuseumId to look in
   * @param obj         The MusitObject to look for
   * @return The augmented object with path, pathNames and currentLocationId
   */
  private def getCurrentLocation(mid: MuseumId, obj: MusitObject): Future[MusitObject] =
    nodeDao.currentLocation(mid, obj.id).flatMap {
      case Some(nodeIdAndPath) =>
        nodeDao.namesForPath(nodeIdAndPath._2).map { pathNames =>
          obj.copy(
            currentLocationId = Some(nodeIdAndPath._1),
            path = Some(nodeIdAndPath._2),
            pathNames = Some(pathNames)
          )
        }
      case None =>
        Future.successful(obj)
    }

  /**
   * Locate object(s) based on museum, old barcode and collection(s).
   *
   * @param mid          The MuseumId to look for objects in.
   * @param oldBarcode   The bar code to look for.
   * @param collections  Which collections to look in.
   * @param currUsr      The currently authenticated user.
   * @return A list of objects that share tha same bare code
   */
  def findByOldBarcode(
    mid: MuseumId,
    oldBarcode: Long,
    collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findByOldBarcode(mid, oldBarcode, collections).flatMap {
      case MusitSuccess(objs) =>
        Future.sequence(objs.map(getCurrentLocation(mid, _)))
          .map(MusitSuccess(_))
          .recover {
            case NonFatal(ex) =>
              val msg = s"An error occured when executing object search by old barcode"
              logger.error(msg, ex)
              MusitInternalError(msg)
          }
      case err: MusitError =>
        Future.successful(err)
    }
  }

  /**
   * Locate objects that share the same main object ID.
   *
   * @param mid           The MuseumId to look for objects in.
   * @param mainObjectId  The main object ID to look for.
   * @param collectionIds Which collections to look in.
   * @param currUsr       The currently authenticated user.
   * @return A list of objects that share the same main object ID.
   */
  def findMainObjectChildren(
    mid: MuseumId,
    mainObjectId: ObjectId,
    collectionIds: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findMainObjectChildren(mid, mainObjectId, collectionIds)
  }

  /**
   * Locate objects in the specified museum, node and collection(s).
   *
   * @param mid           The MuseumId to look for objects in.
   * @param nodeId        The specific StorageNodeDatabaseId to look for objects in.
   * @param collectionIds Specifies collections to fetch objects for.
   * @param page          The page number to retrieve.
   * @param limit         The number of results per page.
   * @param currUsr       The currently authenticated user.
   * @return A list of objects matching the given criteria.
   */
  def findObjects(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId,
    collectionIds: Seq[MuseumCollection],
    page: Int,
    limit: Int
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[PagedResult[MusitObject]]] = {
    objDao.pagedObjects(mid, nodeId, collectionIds, page, limit)
  }

  /**
   * Search for objects based on the given criteria.
   *
   * @param mid           The MuseumId to search for objects in
   * @param collectionIds The collections to search for objects in.
   * @param page          The page number to retrieve.
   * @param limit         The number of results per page.
   * @param museumNo      The MuseumNo to find matches for.
   * @param subNo         The SubNo to find matches for.
   * @param term          The object term to find matches for.
   * @param currUsr       The currently authenticated user.
   * @return A list of search results matching the given criteria.
   */
  def search(
    mid: MuseumId,
    collectionIds: Seq[MuseumCollection],
    page: Int,
    limit: Int,
    museumNo: Option[MuseumNo],
    subNo: Option[SubNo],
    term: Option[String]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectSearchResult]] = {
    objDao.search(mid, page, limit, museumNo, subNo, term, collectionIds).flatMap {
      case MusitSuccess(searchResult) =>
        // We found some objects...now we need to find the current location for each.
        Future.sequence(searchResult.matches.map(getCurrentLocation(mid, _)))
          .map { objects =>
            MusitSuccess(searchResult.copy(matches = objects))
          }.recover {
            case NonFatal(ex) =>
              val msg = s"An error occured when executing object search"
              logger.error(msg, ex)
              MusitInternalError(msg)
          }

      case err: MusitError =>
        Future.successful(err)
    }
  }
}
