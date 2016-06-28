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
import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservice.event.service.BaseService
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}

class EventResource extends Controller {

  def postEvent: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val maybeEventResult = ResourceHelper.jsResultToMusitResult(request.body.validate[Event])
    ResourceHelper.postRootWithMusitResult(BaseService.insertAndGetNewEvent, maybeEventResult, (event: Event) => Json.toJson(event))
  }

  def getEvent(id: Long) = Action.async { request =>
    ResourceHelper.getRoot(BaseService.getEvent, id, (event: Event) => Json.toJson(event))
  }
}