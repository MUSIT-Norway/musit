/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.event.resource

import no.uio.musit.microservice.event.domain.{ Event, EventType }
import no.uio.musit.microservice.event.domain.EventType._
import no.uio.musit.microservice.event.service.EventService
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions.MusitFuture
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ResourceHelper }
import no.uio.musit.microservices.common.utils.Misc._
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{ Action, BodyParsers, Controller, Result }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.extensions.EitherExtensions._

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global

class EventResource extends Controller {
  private def eventToJson(event: Event) = event.toJson

  def postEvent: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val evtTypeName = (request.body \ "eventType").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => eventType.makeEvent(request.body.asInstanceOf[JsObject]) |> ResourceHelper.jsResultToMusitResult
    }

    ResourceHelper.postRootWithMusitResult(EventService.insertAndGetNewEvent, maybeEventResult, eventToJson)

  }

  def getEvent(id: Long) = Action.async { request =>
    ResourceHelper.getRoot(EventService.getEvent, id, eventToJson)
  }

  /*




    EventType(evtTypeName) match {
      case Some(eventType) =>
        eventType.reads(request.body).asEither match {
          case Right(event) =>
            EventService.insertEvent(event).map { created =>
              Ok(Json.toJson(created))
            }
          case Left(error) =>
            Logger.error(error.mkString)
            Future.successful(BadRequest(s"Invalid payload"))
        }
      case None =>
        Future.successful(BadRequest(s"Invalid eventTupe $evtTypeName"))
    }

    request.body.validate[Event].asEither match {
      case Right(event) =>
        EventService.insertEvent(event).map { created =>
          Ok(Json.toJson(created))
        }
      case Left(error) =>
        Future.successful(
          BadRequest(error.map(e => e._1.toJsonString -> e._2.fold("")((a1, a2) => a1 + ", " + a2)).mkString(","))
        )
    }
  }

  */
}
