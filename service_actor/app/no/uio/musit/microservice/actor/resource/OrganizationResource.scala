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

import no.uio.musit.microservice.actor.domain.Organization
import no.uio.musit.microservice.actor.service.OrganizationService
import no.uio.musit.microservices.common.domain.{MusitError, MusitSearch}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class OrganizationResource extends Controller with OrganizationService {


  def listRoot(search: Option[MusitSearch]) = Action.async { request =>
    // TODO: Add searching
    search match {
      case Some(criteria) => Future.successful(NotImplemented("Add search support"))
      case None => all.map(person => { NotImplemented(Json.toJson(person)) }) // Ok
    }
  }

  def getRoot(id:Long) = Action.async { request =>
    find(id).map {
      case Some(person) => NotImplemented(Json.toJson(person)) // Ok
      case None => NotFound(Json.toJson(MusitError(404, s"Didn't find object with id: $id")))
    }
  }

  def postRoot = Action.async(BodyParsers.parse.json) { request =>
    val actorResult:JsResult[Organization] = request.body.validate[Organization]
    actorResult match {
      case s:JsSuccess[Organization] => {
        val org = s.get
        create(org).map { newOrg =>
          NotImplemented(Json.toJson(newOrg)) // Created
        }
      }
      case e:JsError => Future.successful(BadRequest(Json.toJson(MusitError(400, e.toString))))
    }
  }

  def updateRoot = Action.async(BodyParsers.parse.json) { request =>
    val actorResult:JsResult[Organization] = request.body.validate[Organization]
    actorResult match {
      case s:JsSuccess[Organization] => {
        val org = s.get
        update(org).map { newOrg =>
          NotImplemented(Json.toJson(newOrg)) // Ok
        }
      }
      case e:JsError => Future.successful(BadRequest(Json.toJson(MusitError(400, e.toString))))
    }
  }

  def deleteRoot(id:Long) = Action.async { request =>
    remove(id).map {
      case Some(org) => NotImplemented(Json.toJson(org)) // Ok
      case None => NotFound(Json.toJson(MusitError(404, s"Didn't find object with id: $id")))
    }
  }
}
