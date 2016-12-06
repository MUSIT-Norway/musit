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
import no.uio.musit.models.{MuseumId, NamedPathElement, StorageNodeDatabaseId}
import no.uio.musit.service.MusitResults.{MusitError, MusitResult, MusitSuccess}
import repositories.dao.{ObjectDao, StorageNodeDao}

import scala.concurrent.Future

class StorageNodeService @Inject() (
    val nodeDao: StorageNodeDao,
    val objDao: ObjectDao
) {

  /**
   * Checks if the node exists in the storage facility for given museum ID.
   *
   * @param mid MuseumId
   * @param nodeId StorageNodeId
   * @return
   */
  def nodeExists(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Boolean]] = nodeDao.nodeExists(mid, nodeId)

  /**
   *
   * @param mid
   * @param oldObjectId
   * @param oldSchemaName
   * @return
   */
  def currNodeForOldObject(
    mid: MuseumId,
    oldObjectId: Long,
    oldSchemaName: String
  ): Future[MusitResult[Option[(StorageNodeDatabaseId, String)]]] = {
    // Look up object using it's old object ID and the old DB schema name.
    objDao.findByOldId(mid, oldObjectId, oldSchemaName).flatMap {
      case MusitSuccess(mobj) =>
        mobj match {
          case Some(obj) =>
            nodeDao.currentLocation(mid, obj.id).flatMap {
              case Some(sn) =>
                nodeDao.namesForPath(sn._2).map { np =>
                  MusitSuccess(Option((sn._1, np.map(_.name).mkString(", "))))
                }

              case None =>
                Future.successful(MusitSuccess(None))
            }

          case None =>
            Future.successful(MusitSuccess(None))
        }

      case err: MusitError =>
        Future.successful(err)
    }
  }

}
