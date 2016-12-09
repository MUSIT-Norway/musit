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
import controllers.SimpleNode
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.{ObjectDao, StorageNodeDao}

import scala.concurrent.Future

class StorageNodeService @Inject() (
    val nodeDao: StorageNodeDao,
    val objDao: ObjectDao
) {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * Checks if the node exists in the storage facility for given museum ID.
   *
   * @param mid    MuseumId
   * @param nodeId StorageNodeId
   * @return
   */
  def nodeExists(
    mid: MuseumId,
    nodeId: StorageNodeDatabaseId
  ): Future[MusitResult[Boolean]] = nodeDao.nodeExists(mid, nodeId)

  /**
   *
   * @param oldObjectId
   * @param oldSchemaName
   * @return
   */
  def currNodeForOldObject(
    oldObjectId: Long,
    oldSchemaName: String
  )(
    implicit
    currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[(StorageNodeDatabaseId, String)]]] = {
    // Look up object using it's old object ID and the old DB schema name.
    objDao.findByOldId(oldObjectId, oldSchemaName).flatMap {
      case MusitSuccess(mobj) =>
        mobj match {
          case Some(obj) =>
            nodeDao.currentLocation(obj.museumId, obj.id).flatMap {
              case Some(sn) =>
                nodeDao.namesForPath(sn._2).map { np =>
                  // Only authorized users are allowed to see the full path
                  // TODO: We probably need to verify the _group_ and not the museum.
                  if (currUsr.isAuthorized(obj.museumId)) {
                    MusitSuccess(Option((sn._1, np.map(_.name).mkString(", "))))
                  } else {
                    MusitSuccess(Option((sn._1, Museum.museumIdToString(obj.museumId))))
                  }
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

  def nodesOutsideMuseum(museumId: MuseumId): Future[MusitResult[Seq[SimpleNode]]] = {
    nodeDao.getRootLoanNodes(museumId).flatMap {
      case MusitSuccess(rids) =>
        logger.debug(s"Found ${rids.size} external Root nodes: ${rids.mkString(", ")}")
        if (rids.nonEmpty) nodeDao.listAllChildrenFor(museumId, rids)
        else Future.successful(MusitSuccess(Seq.empty))
      case err: MusitError => Future.successful(err)
    }
  }

}
