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

import com.google.inject.{Inject, Singleton}
import no.uio.musit.microservice.storagefacility.domain.Museum
import no.uio.musit.microservice.storagefacility.domain.event.control.Control
import no.uio.musit.microservice.storagefacility.domain.event.observation.Observation
import no.uio.musit.microservice.storagefacility.service.{ControlService, ObservationService}
import no.uio.musit.service.MusitResults.{MusitError, MusitSuccess}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class EventResource @Inject() (
    val controlService: ControlService,
    val observationService: ObservationService
) extends Controller {

  val logger = Logger(classOf[EventResource])

  // TODO: Use user from an enriched request type in a proper SecureAction
  import no.uio.musit.microservice.storagefacility.DummyData.DummyUser

  /**
   * Controller endpoint for adding a new Control for a storage node with
   * the given nodeId.
   */
  def addControl(mid: Int, nodeId: Long) = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    Museum.fromMuseumId(mid).map { museum =>
      request.body.validate[Control] match {
        case JsSuccess(ctrl, jsPath) =>
          controlService.add(mid, nodeId, ctrl).map {
            case MusitSuccess(addedCtrl) =>
              Created(Json.toJson(addedCtrl))

            case err: MusitError =>
              InternalServerError(Json.obj("message" -> err.message))
          }
        case JsError(errors) =>
          Future.successful(BadRequest(JsError.toJson(errors)))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Controller endpoint for adding a new Observation for a storage node with
   * the given nodeId.
   */
  def addObservation(mid: Int, nodeId: Long) = Action.async(parse.json) { implicit request =>
    // TODO: Extract current user information from enriched request.
    Museum.fromMuseumId(mid).map { museum =>
      request.body.validate[Observation] match {
        case JsSuccess(obs, jsPath) =>
          observationService.add(mid, nodeId, obs).map {
            case MusitSuccess(addedObs) =>
              Created(Json.toJson(addedObs))

            case err: MusitError =>
              InternalServerError(Json.obj("message" -> err.message))
          }
        case JsError(errors) =>
          Future.successful(BadRequest(JsError.toJson(errors)))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Fetch a Control with the given eventId for a storage node where the id is
   * equal to the provided nodeId.
   */
  def getControl(mid: Int, nodeId: Long, eventId: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museum =>
      controlService.findBy(mid, eventId).map {
        case MusitSuccess(maybeControl) =>
          maybeControl.map { ctrl =>
            Ok(Json.toJson(ctrl))
          }.getOrElse {
            NotFound
          }

        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Fetch an Observation with the given eventId for a storage node where the
   * id is equal to the provided nodeId.
   */
  def getObservation(mid: Int, nodeId: Long, eventId: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museum =>
      observationService.findBy(mid, eventId).map {
        case MusitSuccess(maybeObservation) =>
          maybeObservation.map { obs =>
            Ok(Json.toJson(obs))
          }.getOrElse {
            NotFound
          }
        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Lists all Controls for the given nodeId
   */
  def listControls(mid: Int, nodeId: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museum =>
      controlService.listFor(mid, nodeId).map {
        case MusitSuccess(controls) =>
          Ok(Json.toJson(controls))

        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Lists all Observations for the given nodeId
   */
  def listObservations(mid: Int, nodeId: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museum =>
      observationService.listFor(mid, nodeId).map {
        case MusitSuccess(observations) =>
          Ok(Json.toJson(observations))

        case err: MusitError =>
          InternalServerError(Json.obj("message" -> err.message))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

  /**
   * Returns a mixed list of controls and observations for a storage node with
   * the given nodeId.
   */
  def listEventsForNode(mid: Int, nodeId: Long) = Action.async { implicit request =>
    Museum.fromMuseumId(mid).map { museum =>
      val eventuallyCtrls = controlService.listFor(mid, nodeId)
      val eventyallyObs = observationService.listFor(mid, nodeId)
      for {
        ctrlRes <- eventuallyCtrls
        obsRes <- eventyallyObs
      } yield {
        val sortedRes = for {
          controls <- ctrlRes
          observations <- obsRes
        } yield {
          controls.union(observations).sortBy(_.doneDate.getMillis)
        }

        sortedRes match {
          case MusitSuccess(sorted) =>
            val jsObjects = sorted.map {
              case ctrl: Control => Json.toJson(ctrl)
              case obs: Observation => Json.toJson(obs)
              case _ => JsNull
            }
            logger.debug(s"Going to return sorted JSON:\n${Json.prettyPrint(JsArray(jsObjects))}")
            Ok(JsArray(jsObjects))

          case err: MusitError =>
            InternalServerError(Json.obj("message" -> err.message))
        }

      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("message" -> s"Unknown museum $mid")))
    }
  }

}
