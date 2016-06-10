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
import no.uio.musit.microservice.event.service.EventService
import io.swagger.annotations.ApiOperation
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.utils.ResourceHelper
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservice.event.domain.{ AtomLink, EventInfo, EventType }
import no.uio.musit.microservice.event.service.EventService
import play.api.libs.json._
import play.api.mvc.{ Action, BodyParsers, Controller, Result }

/**
 * Created by jstabel on 6/10/16.
 */

class EventResource extends Controller {

  def eventInfoToJson(eventInfo: EventInfo) = Json.toJson(eventInfo)

  @ApiOperation(value = "Event operation - inserts an Event", httpMethod = "POST")
  def postRoot: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>

    val eventInfoResult = fromJsonToEventInfo(request.body)
    eventInfoResult.mapToFinalPlayResult { eventInfo =>
      ResourceHelper.postRoot(EventService.createEvent, eventInfo, eventInfoToJson)
    }
  }

  /*
      def getById(id: Long) = Action.async {
        request =>
          ResourceHelper.getRootFromEither(EventService.getById, id, eventInfoToJson)
      }
  */

  def fromJsonToEventInfo(json: JsValue): Either[MusitError, EventInfo] = {
    // TODO: Make this with proper error handling, this is just a quick and dirty version! 
    val eventType = (json \ "eventType").as[String]
    val optJsObject = (json \ "eventData").toOption.map(_.as[JsObject])
    val links = (json \ "links").as[List[AtomLink]]

    Right(EventInfo(eventType, links, optJsObject))
  }

}
