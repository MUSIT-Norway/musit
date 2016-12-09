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
import models.Organisation
import no.uio.musit.security.Authenticator
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.service.{MusitController, MusitSearch}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.OrganisationService

import scala.concurrent.Future

class OrganisationController @Inject() (
    val authService: Authenticator,
    val orgService: OrganisationService
) extends MusitController {

  def search(
    museumId: Int,
    search: Option[MusitSearch]
  ) = MusitSecureAction().async { request =>
    search match {
      case Some(criteria) =>
        orgService.find(criteria).map(orgs => Ok(Json.toJson(orgs)))

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Search criteria is required"))
        )
    }
  }

  def get(id: Long) = MusitSecureAction().async { request =>
    orgService.find(id).map {
      case Some(person) =>
        Ok(Json.toJson(person))

      case None =>
        NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
    }
  }

  def add = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Organisation] match {
      case s: JsSuccess[Organisation] =>
        orgService.create(s.get).map(org => Created(Json.toJson(org)))

      case e: JsError =>
        Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

  def update(id: Long) = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[Organisation] match {
      case s: JsSuccess[Organisation] =>
        orgService.update(s.get).map {
          case MusitSuccess(upd) =>
            upd match {
              case Some(updated) =>
                Ok(Json.obj("message" -> "Record was updated!"))
              case None =>
                Ok(Json.obj("message" -> "No records were updated!"))
            }

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }

      case e: JsError =>
        Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

  def delete(id: Long) = MusitSecureAction().async { request =>
    orgService.remove(id).map { noDeleted =>
      Ok(Json.obj("message" -> s"Deleted $noDeleted record(s)."))
    }
  }
}
