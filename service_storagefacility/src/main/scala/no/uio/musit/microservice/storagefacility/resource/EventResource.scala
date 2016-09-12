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

package no.uio.musit.microservice.storagefacility.resource

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.storagefacility.domain.event.control.Control
import no.uio.musit.microservice.storagefacility.service.ControlService
import play.api.Logger
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class EventResource @Inject() (
    val controlService: ControlService
) extends Controller {

  val logger = Logger(classOf[EventResource])

  val dummyUser = "Darth Vader"

  def addControl(nodeId: Int) = Action.async(parse.json) { implicit request =>
    request.body.validate[Control] match {
      case JsSuccess(ctrl, jsPath) =>
        controlService.add(ctrl, dummyUser).flatMap {
          case Right(eventId) =>
            controlService.fetch(eventId).map {
              ???
            }
            ???
          case Left(error) =>
            logger.error(
              s"An error occured when trying to add a Control. " + error.message
            )
            Future.successful(
              InternalServerError(Json.obj("message" -> error.message))
            )
        }
      case JsError(errors) =>
        ???
    }
    ???
  }

  def listControls(nodeId: Int) = Action.async(parse.json) { implicit request =>
    ???
  }
}
