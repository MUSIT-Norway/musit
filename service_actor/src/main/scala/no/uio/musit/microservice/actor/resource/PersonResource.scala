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

import com.google.inject.Inject
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservice.actor.service.PersonService
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class PersonResource @Inject() (personService: PersonService) extends Controller {

  def listRoot(search: Option[MusitSearch]): Action[AnyContent] = Action.async { request =>
    search match {
      case Some(criteria) => personService.find(criteria).map(persons => Ok(Json.toJson(persons)))
      case None => personService.all.map(person => Ok(Json.toJson(person)))
    }
  }

  def getRoot(id: Long): Action[AnyContent] = Action.async { request =>
    personService.find(id).map {
      case Some(person) => Ok(Json.toJson(person)) // Ok
      case None => NotFound(Json.toJson(MusitError(NOT_FOUND, s"Did not find object with id: $id")))
    }
  }

  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val personResult: JsResult[Person] = request.body.validate[Person]
    personResult match {
      case s: JsSuccess[Person] =>
        personService.create(s.get).map(newPerson => Created(Json.toJson(newPerson)))
      case e: JsError => Future.successful(BadRequest(Json.toJson(MusitError(BAD_REQUEST, e.toString))))
    }
  }

  def updateRoot(id: Long): Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val personResult: JsResult[Person] = request.body.validate[Person]
    personResult match {
      case s: JsSuccess[Person] =>
        personService.update(s.get).map {
          case Right(updateStatus) => Ok(Json.toJson(updateStatus))
          case Left(error) => Status(error.status)(Json.toJson(error))
        }
      case e: JsError => Future.successful(BadRequest(Json.toJson(MusitError(BAD_REQUEST, e.toString))))
    }
  }

  def deleteRoot(id: Long): Action[AnyContent] = Action.async { request =>
    personService.remove(id).map { noDeleted => Ok(Json.toJson(noDeleted)) }
  }
}
