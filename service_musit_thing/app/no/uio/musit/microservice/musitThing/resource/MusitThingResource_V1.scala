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
import io.swagger.annotations._
import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservice.musitThing.service.MusitThingService
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Api(
  value = "/api/musitThing",
  description = "MusitTHing resource, showing how you can put simple methods straight into the resource and do complex logic in traits outside."
)
class MusitThingResource_V1 extends Controller with MusitThingService {
  @ApiOperation(value = "MusitThing operation - lists all musitThings", notes = "simple listing in json", httpMethod = "GET")
  def list = Action.async {
    MusitThingDao.all().map(musitThing => Ok(Json.toJson(musitThing)))
  }

  @ApiOperation(value = "MusitThing operation - get a spesific musitThing", notes = "simple listing in json", httpMethod = "GET")
  def getById(id: Long) = Action.async { request =>

    val filterListe = Seq("displayid", "displayname")

    val filter = extractFilterFromRequest(request)
    if (filter.length == 1) {
      filter(0).toLowerCase match {
        case "displayid" => wrapWithOkOrFailString(id, MusitThingDao.getDisplayID(id))
        case "displayname" => wrapWithOkOrFailString(id, MusitThingDao.getDisplayName(id))
        case whatever => Future(BadRequest(s"Unknown filter:$whatever"))
      }

    } else if (filter.length > 1) {
      Future(BadRequest("Filter can not contain more then one attribute"))
    } else {
      wrapWithOkOrFailThing(id ,MusitThingDao.getById(id))
    }
  }

  @ApiOperation(value = "MusitThing operation - inserts an MusitThingTuple", notes = "simple json parsing and db insert", httpMethod = "POST")
  def add = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[MusitThing] match {
      case s: JsSuccess[MusitThing] => {
        val musitThing = s.get
        val newThingF = MusitThingDao.insert(musitThing)
        newThingF.map { newThing =>
          Created(Json.toJson(newThing))
        }
      }
      case e: JsError => Future(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(e))))
    }
  }

  def wrapWithOkOrFailString(id: Long, eventualMaybeResult: Future[Option[String]]) = {
    eventualMaybeResult.map(
      maybeResult => maybeResult.map(
        result => Ok(Json.toJson(result))
      ).getOrElse(
        NotFound(s"Didn't find object with id: $id")
      )
    )
  }

  def wrapWithOkOrFailThing(id: Long, eventualMaybeResult: Future[Option[MusitThing]]) = {
    eventualMaybeResult.map(
      maybeResult => maybeResult.map(
        result => Ok(Json.toJson(result))
      ).getOrElse(
        NotFound(s"Didn't find object with id: $id")
      )
    )
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

