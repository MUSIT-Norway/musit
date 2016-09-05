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

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageType
import no.uio.musit.microservice.storageAdmin.service.{ BuildingService, RoomService, StorageUnitService }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class StorageUnitResource @Inject() (
    storageUnitService: StorageUnitService,
    buildingService: BuildingService,
    roomService: RoomService
) extends Controller {

  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val musitResultTriple = ResourceHelper.jsResultToMusitResult(request.body.validate[Storage])
    ResourceHelper.postRootWithMusitResult(storageUnitService.createStorageTriple, musitResultTriple, (triple: Storage) => Json.toJson(triple))
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
    storageUnitService.getChildren(id).map(__ => Ok(Json.toJson(__)))
  }

  def getById(id: Long) = Action.async {
    ResourceHelper.getRoot(storageUnitService.getById, id, (triple: Storage) => Json.toJson(triple))
  }

  def listAll = Action.async {
    storageUnitService.all.flatMap(list => {
      Future.sequence(list.map(node => {

        node.storageType match {
          case StorageType.StorageUnit =>
            Future.successful(Storage.fromDTO(node))
          case StorageType.Building =>
            buildingService.getBuildingById(node.id.get).map(_.fold(Storage.fromDTO(node))(building =>
              Storage.getBuilding(node, building)))
          case StorageType.Room =>
            roomService.getRoomById(node.id.get).map(_.fold(Storage.fromDTO(node))(room =>
              Storage.getRoom(node, room)))
        }
      })).map(__ => Ok(Json.toJson(__)))
    })
  }

  def listRootNode = Action.async {
    def readGroup = "foo" // TODO: Replace with actual groups when security is added!!!
    storageUnitService.rootNodes(readGroup).flatMap(list => {
      Future.sequence(list.map(unit => {
        unit.storageType match {
          case StorageType.StorageUnit =>
            Future.successful(Storage.fromDTO(unit))
          case StorageType.Building =>
            buildingService.getBuildingById(unit.id.get).map(_.fold(Storage.fromDTO(unit))(building =>
              Storage.getBuilding(unit, building)))
          case StorageType.Room =>
            roomService.getRoomById(unit.id.get).map(_.fold(Storage.fromDTO(unit))(room =>
              Storage.getRoom(unit, room)))
        }
      })).map(__ => Ok(Json.toJson(__)))
    })
  }

  def updateRoot(id: Long) = Action.async(BodyParsers.parse.json) {
    request =>
      request.body.validate[Storage].asEither match {
        case Right(storage) =>
          storageUnitService.updateStorageTripleByID(id, storage).flatMap {
            case Right(1) => ResourceHelper.getRoot(storageUnitService.getById, id, (triple: Storage) => Json.toJson(triple))
            case Right(n) => Future.successful(NotFound)
            case Left(error) => Future.successful(Status(error.status)(Json.toJson(error)))
          }
        case Left(error) =>
          Future.successful(Status(400)(Json.toJson(MusitError(message = error.mkString))))
      }

  }

  def deleteRoot(id: Long) = Action.async {
    ResourceHelper.deleteRoot(storageUnitService.deleteStorageTriple, id)
  }
}

