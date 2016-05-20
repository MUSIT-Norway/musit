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
package no.uio.musit.microservice.storageAdmin.service

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.StorageUnit
import no.uio.musit.microservices.common.domain.MusitError
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait StorageUnitService {
  def insert(jsonStorage: JsResult[StorageUnit]): Future[Either[MusitError, StorageUnit]] = {
    jsonStorage match {
      case s: JsSuccess[StorageUnit] => {
        val storageUnit = s.get
        val newStorageUnitF = StorageUnitDao.insert(storageUnit)
        newStorageUnitF.map { newStorageUnit =>
          Right(newStorageUnit)
        }
      }
      case e: JsError => Future.successful(Left(MusitError(400, e.toString)))
    }
  }


  /*def getUnderlyingNodes(id: Long) = Action.async { request =>
    StorageUnitDao.getNodes(id).map {
      storageUnits => Ok(Json.toJson(storageUnits))
    }.recover {
      case e => NotFound(s"Didn't find object with id: $id")
    }
  }


  def getUNodes(id: Long): Future[Seq[StorageUnit]] = Action.async { request =>
    StorageUnitDao.getNodes(id).map {
      case storageUnits => Ok(Json.toJson(storageUnits))
      case _ => NotFound(s"Didn't find object with id: $id")
    }
  }
}*/


}

  trait RoomService


  trait BuildingService




