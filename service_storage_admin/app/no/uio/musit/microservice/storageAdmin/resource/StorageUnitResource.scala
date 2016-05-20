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
package no.uio.musit.microservice.storageAdmin.resource

import io.swagger.annotations.ApiOperation
import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.StorageUnit
import no.uio.musit.microservice.storageAdmin.service.StorageUnitService
import no.uio.musit.microservices.common.domain.{MusitFilter, MusitSearch}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorageUnitResource extends Controller with StorageUnitService {

  @ApiOperation(value = "StorageUnit operation - inserts an StorageUnitTuple", notes = "simple json parsing and db insert", httpMethod = "POST")
  def add = Action.async(BodyParsers.parse.json) { request =>
    val storageUnitResult = request.body.validate[StorageUnit]
    insert(storageUnitResult).map {
      case Right(newStorageUnit) => Created(Json.toJson(newStorageUnit))
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }


  /*def now(filter: Option[MusitFilter], search: Option[MusitSearch]) = Action.async { request =>
    Future.successful(
      convertToNow(filter) match {
        case Right(mt) => Ok(Json.toJson(mt))
        case Left(err) => Status(err.status)(Json.toJson(err))
      }
    )
  }
*/

  def getUnderlyingNodes(id: Long) = Action.async { request =>
    StorageUnitDao.getNodes(id).map {
      storageUnits => Ok(Json.toJson(storageUnits))
    }.recover {
      case e => NotFound(s"Didn't find object with id: $id")
    }
  }


  def now(filter: Option[MusitFilter], search: Option[MusitSearch]) = Action.async {
    Future.successful(NotImplemented("foo"))
  }


}