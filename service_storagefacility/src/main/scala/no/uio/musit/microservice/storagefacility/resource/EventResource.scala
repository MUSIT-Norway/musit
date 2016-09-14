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
import no.uio.musit.microservice.storagefacility.domain.event.control.ControlSubEventFormats._
import no.uio.musit.microservice.storagefacility.domain.event.observation.Observation
import no.uio.musit.microservice.storagefacility.domain.event.observation.ObservationSubEventFormats._
import no.uio.musit.microservice.storagefacility.service.{ ControlService, ObservationService }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsError, JsNull, JsSuccess, Json }
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class EventResource @Inject() (
    val controlService: ControlService,
    val observationService: ObservationService
) extends Controller {

  val logger = Logger(classOf[EventResource])

  // TODO: Use user from an enriched request type in a proper SecureAction
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
   * Controller endpoint for adding a new Observation for a storage node with
   * the given nodeId.
   */
  def addObservation(nodeId: Long) = Action.async(parse.json) { implicit request =>
    request.body.validate[Observation] match {
      case JsSuccess(obs, jsPath) =>
        observationService.add(obs, dummyUser).map {
          case MusitSuccess(addedObs) =>
            Ok(Json.toJson(addedObs))

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
  def getControl(nodeId: Long, eventId: Long) = Action.async { implicit request =>
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
   * Fetch an Observation with the given eventId for a storage node where the
   * id is equal to the provided nodeId.
   */
  def getObservation(nodeId: Long, eventId: Long) = Action.async { implicit request =>
    observationService.findBy(eventId).map {
      case MusitSuccess(maybeObservation) =>
        maybeObservation.map { obs =>
          Ok(Json.toJson(obs))
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
  def listControls(nodeId: Long) = Action.async { implicit request =>
    // TODO: Implement controlService that fetches all controls for a nodeId
    controlService.listFor(nodeId).map {
      case MusitSuccess(controls) =>
        Ok(Json.toJson(controls))

      case err: MusitError[_] =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Lists all Observations for the given nodeId
   */
  def listObservations(nodeId: Long) = Action.async { implicit request =>
    observationService.listFor(nodeId).map {
      case MusitSuccess(observations) =>
        Ok(Json.toJson(observations))

      case err: MusitError[_] =>
        InternalServerError(Json.obj("message" -> err.message))
    }
  }

  /**
   * Returns a mixed list of controls and observations for a storage node with
   * the given nodeId.
   */
  def listEventsForNode(nodeId: Long) = Action.async { implicit request =>
    for {
      ctrlRes <- controlService.listFor(nodeId)
      obsRes <- observationService.listFor(nodeId)
    } yield {
      val sortedRes = for {
        controls <- ctrlRes
        observations <- obsRes
      } yield {
        controls.union(observations).sortBy(_.baseEvent.doneDate.getMillis)
      }

      sortedRes match {
        case MusitSuccess(sorted) =>
          val jsObjects = sorted.map {
            case ctrl: Control => Json.toJson(ctrl)
            case obs: Observation => Json.toJson(obs)
            case _ => JsNull
          }
          Ok(Json.arr(jsObjects))

        case err: MusitError[_] =>
          InternalServerError(Json.obj("message" -> err.message))
      }

    }
  }
}
