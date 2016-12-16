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
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.MusitResults._
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

  def findByOldObjectIds(
    oldSchema: String,
    oldObjectIds: Seq[Long]
  ): Future[MusitResult[Seq[ObjectId]]] = {
    objDao.findObjectIdsForOld(oldSchema, oldObjectIds)
  }

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
              val msg = s"An error occured when executing object search"
              logger.error(msg, ex)
              MusitInternalError(msg)
          }
      case err: MusitError =>
        Future.successful(err)
    }
  }

  def findMainObjectChildren(
    mid: MuseumId,
    mainObjectId: ObjectId,
    collectionIds: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findMainObjectChildren(mid, mainObjectId, collectionIds)
  }

  /**
   *
   * @param mid
   * @param nodeId
   * @param collectionIds
   * @param currUsr
   * @return
   */
  def findObjects(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId,
    collectionIds: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findObjects(mid, nodeId, collectionIds)
  }

  /**
   * Search for objects based on the given criteria.
   *
   * @param mid
   * @param collectionIds
   * @param page
   * @param limit
   * @param museumNo
   * @param subNo
   * @param term
   * @return
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
