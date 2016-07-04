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

import no.uio.musit.microservice.storageAdmin.dao.{ BuildingDao, RoomDao }
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.service.StorageUnitService
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorageUnitResource extends Controller {

  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val musitResultTriple = ResourceHelper.jsResultToMusitResult(request.body.validate[Storage])
    ResourceHelper.postRootWithMusitResult(StorageUnitService.createStorageTriple, musitResultTriple, (triple: Storage) => Json.toJson(triple))
  }

  def validateChildren = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[Seq[Link]].asEither match {
      case Right(list) if list.nonEmpty =>
        // FIXME For the actual implementation see MUSARK-120
        Future.successful(Ok(Json.toJson(list.forall(!_.href.isEmpty))))
      case Left(error) =>
        Future.successful(BadRequest(error.mkString))
    }
  }

  def getChildren(id: Long) = Action.async {
    StorageUnitService.getChildren(id).map(__ => Ok(Json.toJson(__)))
  }

  def getById(id: Long) = Action.async {
    ResourceHelper.getRoot(StorageUnitService.getById, id, (triple: Storage) => Json.toJson(triple))
  }

  def listAll = Action.async {
    StorageUnitService.all.flatMap(list => {
      Future.sequence(list.map(unit => {
        unit.`type` match {
          case StorageType.StorageUnit =>
            Future.successful(Storage.fromDTO(unit))
          case StorageType.Building =>
            BuildingDao.getBuildingById(unit.id.get).map(_.fold(Storage.fromDTO(unit))(building =>
              Storage.getBuilding(unit, building)))
          case StorageType.Room =>
            RoomDao.getRoomById(unit.id.get).map(_.fold(Storage.fromDTO(unit))(room =>
              Storage.getRoom(unit, room)))
        }
      })).map(__ => Ok(Json.toJson(__)))
    })
  }

  def updateRoot(id: Long) = Action.async(BodyParsers.parse.json) {
    request =>
      val musitResultTriple = ResourceHelper.jsResultToMusitResult(request.body.validate[Storage])
      ResourceHelper.updateRootWithMusitResult(StorageUnitService.updateStorageTripleByID, id, musitResultTriple)
  }

  def deleteRoot(id: Long) = Action.async {
    ResourceHelper.deleteRoot(StorageUnitService.deleteStorageTriple, id)
  }

}

