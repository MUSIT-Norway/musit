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

package dao

import com.google.inject.Inject
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import models.MuseumId
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

class StorageNodeDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def nodeExists(mid: MuseumId, nodeId: Long): Future[MusitResult[Boolean]] = {
    db.run(
      sql"""
         select count(*)
         from "MUSARK_STORAGE"."STORAGE_NODE"
         where "MUSEUM_ID" = ${mid.underlying}
         and "STORAGE_NODE_ID" = $nodeId
      """.as[Long].head.map(res => MusitSuccess(res == 1))
    ).recover {
        case NonFatal(e) =>
          MusitDbError(s"Error occurred while checking for node existence for nodeId $nodeId", Some(e))
      }
  }
}
