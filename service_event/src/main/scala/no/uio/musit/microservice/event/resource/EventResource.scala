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
import no.uio.musit.microservice.event.service.{ EventService, JsonEventHelpers }
import no.uio.musit.microservices.common.utils.ResourceHelper
import play.api.libs.json._
import play.api.mvc.{ Action, BodyParsers, Controller, Result }
import no.uio.musit.microservices.common.domain.MusitSearch
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservice.event.service.JsonEventHelpers.JsonEventWriter
import no.uio.musit.security.Security

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class EventResource extends Controller {

  private def eventToJson(event: Event) = JsonEventHelpers.toJson(event, true)

  def postEvent: Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    Security.create(request).flatMap {
      case Left(error) => ResourceHelper.error(error)
      case Right(securityConnection) =>
        val maybeEventResult = JsonEventHelpers.validateEvent(request.body.asInstanceOf[JsObject]) //ResourceHelper.jsResultToMusitResult(request.body.validate[Event])
        ResourceHelper.postRootWithMusitResult(EventService.insertAndGetNewEvent(_: Event, true, securityConnection), maybeEventResult, eventToJson)
    }
  }

  def getEvent(id: Long) = Action.async { request =>
    ResourceHelper.getRoot(EventService.getEvent(_: Long, true), id, eventToJson)
  }

  def getAsMusitResult(search: MusitSearch, keyname: String) = {
    val keyvalue = search.searchMap.get(keyname)
    keyvalue.toMusitResult(MusitError(message = s"Missing required key/fieldname: $keyname in search"))
  }

  def getEvents(optSearch: Option[MusitSearch]) = Action.async { request =>

    val tempResult: MusitFuture[(EventType, String, Long)] = Future {
      optSearch match {
        case Some(search) => {
          for {
            eventTypeName <- getAsMusitResult(search, "eventType")
            relation <- getAsMusitResult(search, "rel")
            id <- getAsMusitResult(search, "id")
            idAsLong <- MusitResult.create(id.toLong)
            eventType <- EventType.getByNameAsMusitResult(eventTypeName)
          } yield (eventType, relation, idAsLong)
        }
        case None => Left(MusitError(message = "Missing search parameters object"))
      }
    }
    println("Etter tempResult")
    val futureEvents = tempResult.musitFutureFlatMap {
      case (eventType, relation, objectId) => getEventsFor(eventType, relation, objectId)

    }

    def eventsToJson(events: Seq[Event]) = Json.toJson(events)
    ResourceHelper.getRoot(futureEvents, eventsToJson)
  }

  def getEventsFor(eventType: EventType, relation: String, id: Long) = {
    EventService.getEventsFor(eventType, relation, id)

  }

}