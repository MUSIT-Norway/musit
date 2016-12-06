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

package controllers

import com.google.inject.Inject
import no.uio.musit.models.ActorId
import no.uio.musit.security.Authenticator
import no.uio.musit.service.{MusitController, MusitSearch}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.ActorService

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PersonController @Inject() (
    val authService: Authenticator,
    val service: ActorService
) extends MusitController {

  val logger = Logger(classOf[PersonController])

  def search(
    museumId: Int,
    search: Option[MusitSearch]
  ) = MusitSecureAction().async { request =>
    search match {
      case Some(criteria) =>
        service.findByName(criteria).map(persons => Ok(Json.toJson(persons)))

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Search criteria is required"))
        )
    }
  }

  def details = MusitSecureAction().async(parse.json) { implicit request =>
    request.body.validate[Set[ActorId]] match {
      case JsSuccess(ids, path) =>
        service.findDetails(ids).map { persons =>
          if (persons.isEmpty) {
            NoContent
          } else {
            Ok(Json.toJson(persons))
          }
        }

      case e: JsError =>
        Future.successful(BadRequest(Json.obj("message" -> e.toString)))
    }
  }

  def get(id: String) = MusitSecureAction().async { implicit request =>
    ActorId.validate(id) match {
      case Success(uuid) =>
        service.findByActorId(uuid).map {
          case Some(actor) =>
            Ok(Json.toJson(actor))

          case None =>
            NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
        }
      case Failure(ex) =>
        logger.debug("Request contained invalid UUID")
        Future.successful {
          BadRequest(Json.obj("message" -> s"Id $id is not a valid UUID"))
        }
    }
  }
}
