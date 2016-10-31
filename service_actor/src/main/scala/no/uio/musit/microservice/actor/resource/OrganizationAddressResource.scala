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
import no.uio.musit.microservice.actor.domain.OrganizationAddress
import no.uio.musit.microservice.actor.service.OrganizationAddressService
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import scala.concurrent.Future

class OrganizationAddressResource @Inject() (
    val authService: Authenticator,
    val orgAdrService: OrganizationAddressService
) extends MusitController {

  def listForOrg(organizationId: Long) = MusitSecureAction().async { request =>
    orgAdrService.all(organizationId).map(addr => Ok(Json.toJson(addr)))
  }

  def get(organizationId: Long, id: Long) = MusitSecureAction().async { request =>
    orgAdrService.find(id).map {
      case Some(addr) =>
        Ok(Json.toJson(addr))

      case None =>
        NotFound(Json.obj("message" -> s"Did not find object with id: $id"))
    }
  }

  def add(organizationId: Long) = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[OrganizationAddress] match {
      case s: JsSuccess[OrganizationAddress] =>
        orgAdrService.create(s.get).map(newAddr => Created(Json.toJson(newAddr)))

      case e: JsError =>
        Future.successful(BadRequest(JsError.toJson(e)))
    }
  }

  def update(organizationId: Long, id: Long) = MusitSecureAction().async(parse.json) { request =>
    request.body.validate[OrganizationAddress] match {
      case s: JsSuccess[OrganizationAddress] =>
        orgAdrService.update(s.get).map {
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

  def delete(organizationId: Long, id: Long) = MusitSecureAction().async { request =>
    orgAdrService.remove(id).map { noDeleted =>
      Ok(Json.obj("message" -> s"Deleted $noDeleted record(s)."))
    }
  }
}
