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
package no.uio.musit.microservice.musitThing.resource

import no.uio.musit.microservice.musitThing.dao.MusitThingDao
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservice.musitThing.service.MusitThingService
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

class MusitThingResource_V1 extends Controller with MusitThingService {

  def list = Action.async {
    MusitThingDao.all.map(musitThing => Ok(Json.toJson(musitThing)))
  }

  def getById(id: Long) = Action.async { request =>
    extractFilterFromRequest(request) match {
      case Array(filter) => filter.toLowerCase match {
        case "displayid" =>
          MusitThingDao.getDisplayID(id).map(_.map(displayId => Ok(Json.toJson(displayId)))
            .getOrElse(NotFound(s"Didn't find displayId for object with id [$id]")))

        case "displayname" =>
          MusitThingDao.getDisplayName(id).map(_.map(displayName => Ok(Json.toJson(displayName)))
            .getOrElse(NotFound(s"Didn't find displayName for object with id: $id")))

        case whatever =>
          Future(BadRequest(s"Unknown filter: $whatever"))
      }

      case array if array.length > 1 =>
        Future(BadRequest("Filter can not contain more than one attribute"))

      case emptyArray =>
        MusitThingDao.getById(id).map(_.map(thing => Ok(Json.toJson(thing)))
          .getOrElse(NotFound(s"Didn't find object with id: $id")))
    }
  }

  def add = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[MusitThing].map(thing => MusitThingDao.insert(thing).map {
      newThing => Created(Json.toJson(newThing))
    }).getOrElse(Future(BadRequest(Json.obj(
      "status" -> 400,
      "message" -> s"Input is not valid: ${request.body}"
    ))))
  }

}

