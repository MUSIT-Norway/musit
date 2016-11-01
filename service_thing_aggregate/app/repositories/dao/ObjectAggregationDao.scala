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
import models._
import no.uio.musit.models._
import no.uio.musit.service.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  val logger = Logger(classOf[ObjectAggregationDao])

  import driver.api._

  def getObjects(
    mid: MuseumId,
    nodeId: StorageNodeId
  ): Future[MusitResult[Seq[ObjectAggregation]]] = {
    implicit val getObject = GetResult(r =>
      ObjectAggregation(
        id = ObjectId(r.nextLong),
        museumNo = MuseumNo(r.nextString),
        subNo = r.nextStringOption.map(SubNo.apply),
        term = r.nextStringOption
      ))
    db.run(
      sql"""
         SELECT "MUSITTHING"."OBJECT_ID", "MUSITTHING"."MUSEUMNO", "MUSITTHING"."SUBNO", "MUSITTHING"."TERM"
         FROM "MUSARK_STORAGE"."LOCAL_OBJECT", "MUSIT_MAPPING"."MUSITTHING"
         WHERE "LOCAL_OBJECT"."MUSEUM_ID" = ${mid.underlying}
         AND "LOCAL_OBJECT"."CURRENT_LOCATION_ID" = ${nodeId.underlying}
         AND "LOCAL_OBJECT"."OBJECT_ID" = "MUSITTHING"."OBJECT_ID";
      """.as[ObjectAggregation].map(MusitSuccess.apply)
    ).recover {
        case e: Exception =>
          val msg = s"Error while retrieving objects for nodeId $nodeId"
          logger.error(msg, e)
          MusitDbError(msg, Some(e))
      }
  }
}
