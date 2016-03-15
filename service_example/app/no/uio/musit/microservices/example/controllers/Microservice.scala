/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package no.uio.musit.microservices.example.controllers

import no.uio.musit.microservices.example.domain.Example
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.swagger.annotations._
import no.uio.musit.microservices.example.dao._

import play.api.mvc.{BodyParsers, Action, Controller}
import play.api.libs.json._

@Api(value = "/api/example", description = "Example service")
class Microservice extends Controller {
  val exampleDao = new ExampleDao

  @ApiOperation(value = "Example operation - lists all examples", notes = "simple listing in json", httpMethod = "GET")
  def list = Action.async {
    exampleDao.all.map( examples =>
      Ok(Json.toJson(examples))
    )
  }

  @ApiOperation(value = "Example operation - inserts an example", notes = "simple json parsin and db insert", httpMethod = "POST")
  def add = Action(BodyParsers.parse.json) { request =>
    val exampleResult = request.body.validate[Example]
    exampleResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"Error", "message" -> JsError.toFlatJson(errors)))
      },
      example => {
        exampleDao.insert(example)
        Created(Json.obj("status" ->"OK", "message" -> (s"Example '${example}' saved.") ))
      }
    )
  }

}
