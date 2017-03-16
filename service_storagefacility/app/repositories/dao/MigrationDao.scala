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

package repositories.dao

import com.google.inject.Inject
import models.storage.dto.StorageNodeDto
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.StorageNodeId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * This Dao should _NOT_ be used by any other class than the boostrapping
 * {{{migration.UUIDVerifier}}} class.
 */
class MigrationDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends StorageTables {

  val logger = Logger(classOf[MigrationDao])

  import profile.api._

  /**
   * It will set a UUID to all storage nodes that are doesn't have one.
   *
   * @return the number of nodes that were updated
   */
  def generateUUIDWhereEmpty: Future[MusitResult[Int]] = {
    // First find all the nodes without the uuid set.
    val q1 = storageNodeTable.filter(_.uuid.isEmpty)
    val fn = db.run(q1.result).map(_.map(StorageNodeDto.toGenericStorageNode))

    fn.flatMap { nodes =>
      logger.info(s"Found ${nodes.size} without UUID.")
      // Now we can iterate over all the found nodes, and set a new UUID to
      // each of them separately.
      Future.sequence {
        nodes.map { n =>
          val uuid = StorageNodeId.generateAsOpt()
          val q2   = storageNodeTable.filter(_.id === n.id).map(_.uuid).update(uuid)
          db.run(q2)
        }
      }.map { allRes =>
        val total = allRes.sum
        logger.info(s"Gave $total storage nodes a new UUID.")
        MusitSuccess(total)
      }
    }.recover {
      case NonFatal(ex) =>
        val msg = "An error occurred setting UUID's to storage nodes."
        logger.error(msg, ex)
        MusitDbError(msg, Option(ex))
    }
  }

}
