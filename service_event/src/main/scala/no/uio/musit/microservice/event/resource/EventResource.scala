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
import java.sql.Date

import no.uio.musit.microservice.event.domain.{ Event, EventType }
import no.uio.musit.microservice.event.service.JsonEventHelpers.JsonEventWriter
import no.uio.musit.microservice.event.service.{ EventService, JsonEventHelpers }
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch }
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ResourceHelper }
import no.uio.musit.security.Security
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{ Action, BodyParsers, Controller, Request }

import scala.concurrent.Future

class EventResource extends Controller {

  private def eventToJson(event: Event) = JsonEventHelpers.toJson(event, true)
  private def eventsToJson(events: Seq[Event]) = Json.toJson(events)

  private def handlePostEvent(request: Request[JsValue], jsonValidator: JsObject => MusitResult[Event]) = {
    Security.create(request).flatMap {
      case Left(error) => ResourceHelper.error(error)
      case Right(securityConnection) =>
        val maybeEventResult = jsonValidator(request.body.asInstanceOf[JsObject])
        ResourceHelper.postRootWithMusitResult(EventService.insertAndGetNewEvent(_: Event, true, securityConnection), maybeEventResult, eventToJson)
    }
  }

  def postEvent: Action[JsValue] = Action.async(BodyParsers.parse.json) { request: Request[JsValue] =>
    handlePostEvent(request, JsonEventHelpers.validateEvent)
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
    val futureEvents = tempResult.musitFutureFlatMap {
      case (eventType, relation, objectId) => getEventsFor(eventType, relation, objectId)

    }
    ResourceHelper.getRoot(futureEvents, eventsToJson)
  }

  def getEventsFor(eventType: EventType, relation: String, id: Long) = {
    EventService.getEventsFor(eventType, relation, id)

  }

  def validateEventTypeIsMissingOrEqualTo(jsObject: JsObject, eventType: EventType): MusitResult[Boolean] = {
    val optEventTypeName = (jsObject \ "eventType").validateOpt[String]
    ResourceHelper.jsResultToMusitResult(optEventTypeName).flatMap {
      case Some(eventTypeName) =>
        boolToMusitBool(
          eventType.hasName(eventTypeName),
          ErrorHelper.badRequest(s"Eventtype mismatch, expecting: ${eventType.name}, got: $eventTypeName")
        )
      case None => musitTrue
    }
  }

  private val storageUnitLocationRel: String = "storageunit-location"

  def addStorageNodeRelationAndEventType(jsObject: JsObject, storageNodeId: Int, eventType: EventType): JsObject = {
    val newObject = JsObject(
      Seq(
        ("eventType" -> JsString(eventType.name)),
        "links" -> JsArray(Seq(JsObject(Seq(
          ("rel" -> JsString(storageUnitLocationRel)),
          ("href" -> JsString(s"storageunit/$storageNodeId"))
        ))))
      )
    )
    jsObject.deepMerge(newObject)
  }

  def postEventRelatedToStorageNode(eventType: EventType, nodeId: Int): Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    def validator(jsObject: JsObject): MusitResult[Event] = {
      validateEventTypeIsMissingOrEqualTo(jsObject, eventType).flatMap { _ =>
        val resultJsObject = addStorageNodeRelationAndEventType(jsObject, nodeId, eventType)
        JsonEventHelpers.validateEvent(resultJsObject)
      }
    }
    handlePostEvent(request, validator)
  }

  val controlEventType = EventType.getByNameOrFail("control")
  val observationEventType = EventType.getByNameOrFail("observation")

  def postControl(nodeId: Int): Action[JsValue] = postEventRelatedToStorageNode(controlEventType, nodeId)

  def postObservation(nodeId: Int): Action[JsValue] = postEventRelatedToStorageNode(observationEventType, nodeId)

  def getControls(nodeId: Int) = Action.async {
    val events = getEventsFor(controlEventType, storageUnitLocationRel, nodeId)
    ResourceHelper.getRoot(events, eventsToJson)
  }

  def getObservations(nodeId: Int) = Action.async {
    val events = getEventsFor(observationEventType, storageUnitLocationRel, nodeId)
    ResourceHelper.getRoot(events, eventsToJson)
  }

  def getControlsAndObservations(nodeId: Int) = Action.async {
    val futControls = getEventsFor(controlEventType, storageUnitLocationRel, nodeId)
    val futObservations = getEventsFor(observationEventType, storageUnitLocationRel, nodeId)

    val futEvents = futControls.musitFutureFlatMap { controls =>
      futObservations.musitFutureMap { observations =>
        import no.uio.musit.microservice.event.service.EventOrderingUtils.eventByRegisteredDateOrdering
        controls.union(observations).sorted(eventByRegisteredDateOrdering)
      }
    }
    ResourceHelper.getRoot(futEvents, eventsToJson)
  }
}