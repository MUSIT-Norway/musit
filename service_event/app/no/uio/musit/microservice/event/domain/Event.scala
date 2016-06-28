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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, MusitResult }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ResourceHelper }
import play.api.libs.json.{ JsObject, JsResult, JsValue, Json }
import slick.dbio.DBIO

case class BaseEventDTO(id: Option[Long], links: Option[Seq[Link]], eventType: Int, note: Option[String])

object BaseEventDTO {
  implicit val format = Json.format[BaseEventDTO]
}

class Event(val eventType: EventType, val baseEventDTO: BaseEventDTO) {
  val id: Option[Long] = baseEventDTO.id
  val note: Option[String] = baseEventDTO.note
  val links: Option[Seq[Link]] = baseEventDTO.links
}

/**
 * We split events into to kinds:
 * 1) Those which store all their data in the base event table. We call these "Simple" events.
 * 2) Those which have extended properties (ie need a separate table of properties), we call these "Complex" events.
 * Complex events need to implement this trait.
 */
trait EventFactory {
  /** creates an Event instance (of proper eventType) from jsObject. The base event data is already read into baseResult */
  def fromJson(eventType: EventType, baseResult: JsResult[BaseEventDTO], jsObject: JsObject): JsResult[Event]

  /** Writes the extended/specific properties to a JsObject */
  def toJson(event: Event): JsValue

  /** reads the extended/specific properties from the database and creates the final event object */
  def fromDatabase(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Event]

  /** creates an action which inserts the extended/specific properties into the database */
  def createDatabaseInsertAction(id: Long, event: Event): DBIO[Int]
}

object EventHelpers {
  private def fromJsonToBaseEventDto(eventType: EventType, jsObject: JsObject): JsResult[BaseEventDTO] = {
    for {
      id <- (jsObject \ "id").validateOpt[Long]
      links <- (jsObject \ "links").validateOpt[Seq[Link]]
      note <- (jsObject \ "note").validateOpt[String]
    } yield BaseEventDTO(id, links, eventType.id, note)
  }

  def fromJsonToEventResult(eventType: EventType, jsObject: JsObject): JsResult[Event] = {
    val baseEventDto = fromJsonToBaseEventDto(eventType, jsObject)
    eventType.eventFactory match {
      case Some(evtController) => evtController.fromJson(eventType, baseEventDto, jsObject)
      case None => baseEventDto.map(dto => new Event(eventType, dto))
    }

  }

  def validateEvent(jsObject: JsObject): MusitResult[Event] = {
    val evtTypeName = (jsObject \ "eventType").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => fromJsonToEventResult(eventType, jsObject) |> ResourceHelper.jsResultToMusitResult
    }
    maybeEventResult
  }

  def eventFromJson[T <: Event](jsValue: JsValue): MusitResult[T] = {
    validateEvent(jsValue.asInstanceOf[JsObject]).map(res => res.asInstanceOf[T])
  }

  def fromDatabaseToEvent(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Event] = {
    eventType.eventFactory match {
      case Some(evtController) => evtController.fromDatabase(eventType, id, baseEventDto)
      case None => MusitFuture.successful(new Event(eventType, baseEventDto))
    }
  }

  def eventFactoryFor(event: Event) = event.eventType.eventFactory

  def toJson(event: Event) = {
    val baseJson = Json.toJson(BaseEventDTOHack.fromBaseEventDto(event.baseEventDTO)).asInstanceOf[JsObject]
    eventFactoryFor(event).fold(baseJson)(evtController => baseJson ++ (evtController.toJson(event).asInstanceOf[JsObject]))
  }
}

//Example of a simple event....
class Move(eventType: EventType, baseDTO: BaseEventDTO) extends Event(eventType, baseDTO)

// -----------------------------

