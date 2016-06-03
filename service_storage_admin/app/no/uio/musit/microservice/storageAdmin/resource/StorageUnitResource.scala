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
import no.uio.musit.microservice.storageAdmin.service.StorageUnitService
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorageUnitResource extends Controller {

  def jsResultToEither[T](jsRes: JsResult[T]) /*: Either[Status, T]*/ = {
    jsRes match {
      case s: JsSuccess[T] => Right(s.value)
      case e: JsError => Left(BadRequest(Json.toJson(e.toString)))
    }
  }

  @ApiOperation(value = "StorageUnit operation - inserts an StorageUnitTuple", notes = "simple json parsing and db insert", httpMethod = "POST")
  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>

    val eitherTriple = fromJsonToStorageUnitTriple(request.body)

    eitherTriple.fold(r => Future.successful(r), triple => ResourceHelper.postRoot(StorageUnitService.createStorageTriple, triple, storageUnitTripleToJson))
  }

  def getChildren(id: Long) = Action.async {
    request =>
      StorageUnitService.getChildren(id).map {
        storageUnits => Ok(Json.toJson(storageUnits))
      }
  }

  def getById(id: Long) = Action.async {
    request =>
      ResourceHelper.getRootFromEither(StorageUnitService.getById, id, storageUnitTripleToJson)
  }

  def listAll = Action.async {
    request =>
      val debugval = StorageUnitService.all
      debugval.map {
        case storageUnits => Ok(Json.toJson(storageUnits))
      }
  }

  def BadMusitRequest(text: String) = BadRequest(Json.toJson(MusitError(BAD_REQUEST, text)))

  def storageUnitTripleToJson(triple: StorageUnitTriple) = triple.toJson

  def fromJsonToStorageUnitTriple(json: JsValue): Either[Result, StorageUnitTriple] = {
    val storageType = (json \ "storageType").as[String]
    StorageUnitType(storageType) match {
      case StUnit => {
        val jsResultStUnit = json.validate[StorageUnit]
        jsResultStUnit.map(StorageUnitTriple.createStorageUnit) |> jsResultToEither

      }
      case Room => {
        val roomResult = {
          for {
            storageUnit <- json.validate[StorageUnit]
            storageRoom <- json.validate[StorageRoom]
          } yield StorageUnitTriple.createRoom(storageUnit, storageRoom)
        }
        roomResult |> jsResultToEither
      }
      case Building => {
        val buildingResult = {
          for {
            storageUnit <- json.validate[StorageUnit]
            storageBuilding <- json.validate[StorageBuilding]
          } yield StorageUnitTriple.createBuilding(storageUnit, storageBuilding)
        }
        buildingResult |> jsResultToEither
      }
    }
  }

  def updateRoot(id: Long) = Action.async(BodyParsers.parse.json) {
    request =>
      {
        val eitherTriple = fromJsonToStorageUnitTriple(request.body)

        eitherTriple.fold(r => Future.successful(r), triple => ResourceHelper.updateRoot(StorageUnitService.updateStorageTripleByID _, id, triple))
      }
  }

}

