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

import io.swagger.annotations.ApiOperation
import no.uio.musit.microservice.event.domain.{ AtomLink, CompleteEvent, EventInfo, EventType }
import no.uio.musit.microservice.event.service.EventService
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ResourceHelper }
import play.api.libs.json._
import play.api.mvc.{ Action, BodyParsers, Controller }

import scala.util.{ Failure, Success, Try }

/**
 * Created by jstabel on 6/10/16.
 */

class EventResource extends Controller {

  def eventInfoToJson(eventInfo: EventInfo) = Json.toJson(eventInfo)

  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val rawEventInfo = EventInfo(request.body)
    val maybeEventInfo = {
      val maybeEventType = rawEventInfo
      maybeEventType match {
        case Success(eventType) => Right(rawEventInfo)
        case Failure(e) => Left(ErrorHelper.badRequest(e.getMessage))
      }
    }
    ResourceHelper.postRootWithMusitResult(EventService.createEvent, maybeEventInfo, eventInfoToJson)
    // TODO parse shit
    // TODO insert shit
  }

  def getRoot(id: Long) = Action.async { request =>
    def completeEventToEventInfoToJson(completeEvent: CompleteEvent) =
      EventService.completeEventToEventInfo(completeEvent) |> eventInfoToJson

    ResourceHelper.getRoot(EventService.getById, id, completeEventToEventInfoToJson)
  }
}
