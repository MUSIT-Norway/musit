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
import no.uio.musit.microservice.storageAdmin.dao.StorageDao
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageDtoConverter
import no.uio.musit.microservice.storageAdmin.service.{ BuildingService, OrganisationService, RoomService, StorageUnitService }
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.{ Misc, ResourceHelper }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class StorageUnitResource @Inject() (
    storageUnitService: StorageUnitService,
    buildingService: BuildingService,
    roomService: RoomService,
    organisationService: OrganisationService,
    storageDao: StorageDao
) extends Controller with StorageDtoConverter {

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

  def getPath(id: Long) = Action.async {
    storageUnitService.getPath(id).map(__ => Ok(Json.toJson(__)))
  }

  def getById(id: Long) = Action.async {
    ResourceHelper.getRoot(storageUnitService.getById, id, (triple: Storage) => Json.toJson(triple))
  }

  def listAll = Action.async {
    val nodes = storageUnitService.all.map {
      list =>
        list.map {
          dtoNode => storageDao.getByNode(dtoNode)
        }
    }
    val res = nodes.flatMap(list => Misc.filterSuccesses(list))
    res.map(x => Ok(Json.toJson(x)))
  }

  def listRootNode = Action.async {
    def readGroup = "foo" // TODO: Replace with actual groups when security is added!!!

    val nodes = storageUnitService.rootNodes(readGroup).map {
      list =>
        list.map {
          dtoNode => storageDao.getByNode(dtoNode)
        }
    }
    val res = nodes.flatMap(list => Misc.filterSuccesses(list))
    res.map(x => Ok(Json.toJson(x)))
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

