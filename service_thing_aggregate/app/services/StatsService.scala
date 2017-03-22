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
import models.NodeStats
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.{StorageNodeDao, StorageStatsDao}

import scala.concurrent.Future

class StatsService @Inject()(
    val nodeDao: StorageNodeDao,
    val statsDao: StorageStatsDao
) {

  val logger = Logger(classOf[StatsService])

  def nodeStats(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[NodeStats]]] = {
    MusitResultT(nodeDao.getPathById(mid, nodeId)).flatMap { maybeNode =>
      maybeNode.map { node =>
        val totalF     = MusitResultT(statsDao.numObjectsInPath(node._2))
        val directF    = MusitResultT(statsDao.numObjectsInNode(node._1))
        val nodeCountF = MusitResultT(statsDao.numChildren(node._1))

        for {
          total     <- totalF
          direct    <- directF
          nodeCount <- nodeCountF
        } yield {
          Option(NodeStats(nodeCount, direct, total))
        }
      }.getOrElse {
        MusitResultT[Future, Option[NodeStats]](Future.successful(MusitSuccess(None)))
      }
    }.value
  }

}
