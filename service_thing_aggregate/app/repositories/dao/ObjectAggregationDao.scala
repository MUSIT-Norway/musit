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
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class ObjectAggregationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends ObjectTables {

  val logger = Logger(classOf[ObjectAggregationDao])

  import driver.api._

  private val objects = TableQuery[ObjectTable]
  private val localObjects = TableQuery[LocalObjectsTable]

  def getObjects(
    mid: MuseumId,
    nodeId: StorageNodeId
  ): Future[MusitResult[Seq[ObjectAggregation]]] = {

    val locObjQuery = localObjects.filter { lo =>
      lo.museumId === mid &&
        lo.currentLocationId === nodeId
    }

    val query = for {
      (_, o) <- locObjQuery join objects on (_.objectId === _.id)
    } yield (o.id, o.museumNo, o.subNo, o.term)

    db.run(query.result).map { objs =>
      objs.map { o =>
        ObjectAggregation(
          id = o._1,
          museumNo = MuseumNo(o._2),
          subNo = o._3.map(SubNo.apply),
          term = Option(o._4)
        )
      }
    }.map(MusitSuccess.apply).recover {
      case e: Exception =>
        val msg = s"Error while retrieving objects for nodeId $nodeId"
        logger.error(msg, e)
        MusitDbError(msg, Some(e))
    }
  }
}
