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
package no.uio.musit.microservice.geoLocation.resource

import io.swagger.annotations._
import no.uio.musit.microservice.geoLocation.dao.GeoLocationDao
import no.uio.musit.microservice.geoLocation.domain.GeoLocation
import no.uio.musit.microservice.geoLocation.service.GeoLocationService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

@Api(value = "/api/geoLocation", description = "GeoLocation resource, showing how you can put simple methods straight into the resource and do complex logic in traits outside.")
class GeoLocationResource_V1 extends Controller with GeoLocationService {

  @ApiOperation(value = "GeoLocation operation - lists all geoLocations", notes = "simple listing in json", httpMethod = "GET")
  def list = Action.async { req => {
    GeoLocationDao.all.map(geoLocation =>
      Ok(Json.toJson(geoLocation))
    )}
  }


  @ApiOperation(value = "GeoLocation operation - get a spesific geoLocation", notes = "simple listing in json", httpMethod = "GET")
  def getById(id:Long) = Action.async { request => {
    GeoLocationDao.getById(id).map( optionResult =>
      optionResult match {
        case Some(geoLocation) => Ok(Json.toJson(geoLocation))
        case None => NotFound(s"Didn't find object with id: $id")
      }
    )
  }}



  @ApiOperation(value = "GeoLocation operation - inserts an GeoLocationTuple", notes = "simple json parsing and db insert", httpMethod = "POST")
  def add = Action.async(BodyParsers.parse.json) { request =>
    val musitThingResult:JsResult[GeoLocation] = request.body.validate[GeoLocation]
    musitThingResult match {
      case s:JsSuccess[GeoLocation] => {
        val musitThing = s.get
        val newThingF = GeoLocationDao.insert(musitThing)
        newThingF.map { newThing =>
          Created(Json.toJson(newThing))
        }
      }
      case e:JsError => Future(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(e))))
    }
  }

  def extractFilterFromRequest(request: Request[AnyContent]): Array[String] = {
    request.getQueryString("filter") match {
      case Some(filterString) => "^\\[(\\w*)\\]$".r.findFirstIn(filterString) match {
        case Some(str) => str.split(",")
        case None => Array.empty[String]
      }
      case None => Array.empty[String]
    }
  }

}



