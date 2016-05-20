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
import no.uio.musit.microservice.actor.domain.Actor
import no.uio.musit.microservice.actor.service.ActorService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

// TODO: Activate new routes and delete old ones for Actor, and rename resource to LegacyPerson + redo the integration tests
class ActorResource_V1 extends Controller with ActorService {


  def list = Action.async { request => {
    // TODO: Extend with MusitSearch support
    ActorDao.allActors.map(actors => {
      Ok(Json.toJson(actors))
    })}
  }

  def getById(id:Long) = Action.async { request => {
    ActorDao.getActorById(id).map {
      case Some(actor) => Ok(Json.toJson(actor))
      case None => NotFound(s"Didn't find object with id: $id")
    }
  }}

  def add = Action.async(BodyParsers.parse.json) { request =>
    val actorResult:JsResult[Actor] = request.body.validate[Actor]
    actorResult match {
      case s:JsSuccess[Actor] => {
        val actor = s.get
        val newActorF = ActorDao.insertActor(actor)
        newActorF.map { newActor =>
          Created(Json.toJson(newActor))
        }
      }
      case e:JsError => Future(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(e))))
    }
  }
}
