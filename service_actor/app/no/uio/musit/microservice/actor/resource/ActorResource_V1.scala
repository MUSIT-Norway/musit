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
package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.dao.ActorDao
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import io.swagger.annotations._
import no.uio.musit.microservice.actor.domain.Actor
import no.uio.musit.microservice.actor.service.ActorService
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ActorResource_V1 extends Controller with ActorService {
  //val musit_thing_Dao = new MusitThingDao

  def list = Action.async { req => {
    //req.getQueryString("filter")
    ActorDao.all.map(actor =>
      Ok(Json.toJson(actor))
    )}
  }

  def getById(id:Long) = Action.async { request => {
    ActorDao.getById(id).map( optionResult =>
      optionResult match {
        case Some(actor) => Ok(Json.toJson(actor))
        case None => NotFound(s"Didn't find object with id: $id")
      }
    )
  }}

  def add = Action.async(BodyParsers.parse.json) { request =>
    val musitThingResult:JsResult[Actor] = request.body.validate[Actor]
    musitThingResult match {
      case s:JsSuccess[Actor] => {
        val musitThing = s.get
        val newThingF = ActorDao.insert(musitThing)
        newThingF.map { newThing =>
          Created(Json.toJson(newThing))
        }
      }
      case e:JsError => Future(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(e))))
    }
  }
}
