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
import no.uio.musit.microservice.storagefacility.domain.MusitResults.{ MusitError, MusitSuccess }
import no.uio.musit.microservice.storagefacility.domain.event.control.Control
import no.uio.musit.microservice.storagefacility.service.ControlService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class EventResource @Inject() (
    val controlService: ControlService
) extends Controller {

  val logger = Logger(classOf[EventResource])

  val dummyUser = "Darth Vader"

  /**
   * Controller endpoint for adding a new Control for a storage node with
   * the given nodeId.
   */
  def addControl(nodeId: Long) = Action.async(parse.json) { implicit request =>
    request.body.validate[Control] match {
      case JsSuccess(ctrl, jsPath) =>
        controlService.add(ctrl, dummyUser).map {
          case MusitSuccess(addedCtrl) =>
            Ok(Json.toJson(addedCtrl))

          case err: MusitError[_] =>
            InternalServerError(Json.obj("message" -> err.message))
        }
      case JsError(errors) =>
        Future.successful(BadRequest(JsError.toJson(errors)))
    }
  }

  /**
   * Fetch a Control with the given eventId for a storage node where the id is
   * equal to the provided nodeId.
   */
  def getControl(
    nodeId: Long,
    eventId: Long
  ) = Action.async(parse.json) { implicit request =>
    controlService.findBy(eventId).map {
      case MusitSuccess(maybeControl) =>
        maybeControl.map { ctrl =>
          Ok(Json.toJson(ctrl))
        }.getOrElse {
          NotFound
        }

      case err: MusitError[_] =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Lists all Controls for the given nodeId
   */
  def listControls(
    nodeId: Long
  ) = Action.async(parse.json) { implicit request =>
    // TODO: Implement controlService that fetches all controls for a nodeId
    controlService.listFor(nodeId)
    ???
  }

  /**
   * Returns a mixed list of controls and observations for a storage node with
   * the given nodeId.
   */
  def listEventsForNode(
    nodeId: Long
  ) = Action.async(parse.json) { implicit request =>
    ???
  }
}
