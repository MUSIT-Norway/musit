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
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.service.{ RoomService, StorageUnitService }
import no.uio.musit.microservices.common.domain.{ MusitFilter, MusitSearch }
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import no.uio.musit.microservices.common.domain.MusitError

class StorageUnitResource extends Controller {

  def unwrapJsResult(jsRes: JsResult[Future[Result]]): Future[Result] = {
    jsRes match {
      case s: JsSuccess[Future[Result]] => s.value
      case e: JsError => Future.successful(BadRequest(Json.toJson(e.toString)))
    }
  }

  def eitherToCreatedOrBadRequestResult[T](either: Either[MusitError, T])(jsonProvider: T => JsValue): Result = {
    either match {
      case Right(obj: T) => Created(jsonProvider(obj))
      case Left(err: MusitError) => BadRequest(Json.toJson(err))
    }
  }


  def mergeJson(jsonA: JsObject, jsonB: JsObject): JsObject = jsonA ++ jsonB

  @ApiOperation(value = "StorageUnit operation - inserts an StorageUnitTuple", notes = "simple json parsing and db insert", httpMethod = "POST")
  def postRoots: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val json = request.body
    val storageType = (json \ "storageType").as[String]
    StorageUnitType(storageType) match {
      case StUnit => {
        val JsResultStUnit = request.body.validate[StorageUnit]
        val jsResStUnit = JsResultStUnit.map { storageUnit =>
          StorageUnitService.create(storageUnit).map {
            case Right(newStorageUnit) => Created(Json.toJson(newStorageUnit))
            case Left(error) => BadRequest(Json.toJson(error))
          }
        }
        unwrapJsResult(jsResStUnit)
      }
      case Room => {
        val result = {
          for {
            storageUnit <- request.body.validate[StorageUnit]
            storageRoom <- request.body.validate[StorageRoom]
          } yield RoomService.create(storageUnit, storageRoom)
        }
        unwrapJsResult(result.map(_.map(either =>
          eitherToCreatedOrBadRequestResult(either) { case (stUnit: StorageUnit, stRoom: StorageRoom) => mergeJson(stUnit.toJson, stRoom.toJson) })))
      }
      /*TODO, some search+replace missing!
      case Building => {
        val result = {
          for {
            storageUnit <- request.body.validate[StorageUnit]
            storageBuilding <- request.body.validate[StorageBuilding]
          } yield RoomService.create(storageUnit, storageBuildign)
        }
        unwrapJsResult(result.map(_.map(either =>
          eitherToCreatedOrBadRequestResult(either) { case (stUnit: StorageUnit, stBuilding: StorageBuilding) => mergeJson(stUnit.toJson, stBuildingRoom.toJson) })))
      }*/
    }
  }

  /*

  val vAnyStorageUnit = request.body.validate[StorageUnit]
  vAnyStorageUnit.map {
    anyStorageUnit =>

    val storageRoomResult = request.body.validate[StorageRoom]
    val storageBuildingResult = request.body.validate[StorageBuilding]
    val res = storageUnitResult.map {
    storageUnit =>
    StorageUnitService.create (storageUnit).map {
    case Right (newStorageUnit) => Created (Json.toJson (newStorageUnit) )
    case Left (error) => BadRequest (Json.toJson (error) )
  }
  }
    UnwrapJsResult (res)
  }
  */

  def postRoot = Action.async(BodyParsers.parse.json) {
    request =>
      val storageUnitResult = request.body.validate[StorageUnit]
      val res = storageUnitResult.map {
        storageUnit =>
          StorageUnitService.create(storageUnit).map {
            case Right(newStorageUnit) => Created(Json.toJson(newStorageUnit))
            case Left(error) => BadRequest(Json.toJson(error))
          }
      }
      unwrapJsResult(res)
  }

  def getChildren(id: Long) = Action.async {
    request =>
      StorageUnitService.getChildren(id).map {
        storageUnits => Ok(Json.toJson(storageUnits))
      }
  }

  def getById(id: Long) = Action.async {
    request =>
      StorageUnitService.getById(id).map {
        case Some(storageUnit) => Ok(Json.toJson(storageUnit))
        case None => NotFound(Json.toJson(MusitError(404, s"Didn't find storage unit with id: $id")))
      }
  }

  def now(filter: Option[MusitFilter], search: Option[MusitSearch]) = Action.async {
    Future.successful(NotImplemented("foo"))
  }

}