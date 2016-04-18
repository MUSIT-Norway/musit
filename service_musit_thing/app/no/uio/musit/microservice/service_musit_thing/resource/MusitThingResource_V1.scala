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
package no.uio.musit.microservice.service_musit_thing.resource

import no.uio.musit.microservice.service_musit_thing.dao.MusitThingDao
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.swagger.annotations._
import no.uio.musit.microservice.service_musit_thing.domain.MusitThing
import no.uio.musit.microservice.service_musit_thing.service.MusitThingService
import play.api.mvc.{Action, BodyParsers, Controller}
import play.api.libs.json._

@Api(value = "/api/example", description = "Example resource, showing how you can put simple methods straight into the resource and do complex logic in traits outside.")
class MusitThingResource_V1 extends Controller with MusitThingService {
  val musit_thing_Dao = new MusitThingDao

  @ApiOperation(value = "Example operation - lists all examples", notes = "simple listing in json", httpMethod = "GET")
  def list = Action.async {
    musit_thing_Dao.all.map( examples =>
      Ok(Json.toJson(examples))
    )
  }

  @ApiOperation(value = "Example operation - inserts an example", notes = "simple json parsing and db insert", httpMethod = "POST")
  def add = Action(BodyParsers.parse.json) { request =>
    val musit_thing_result = request.body.validate[MusitThing]
    musit_thing_result.fold(
      errors => {
        BadRequest(Json.obj("status" ->"Error", "message" -> JsError.toJson(errors)))
      },
      musit_thing_tuple => {
        musit_thing_Dao.insert(musit_thing_tuple)
        Created(Json.obj("status" ->"OK", "message" -> (s"Example '${musit_thing_tuple}' saved.") ))
      }
    )
  }

}
