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
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, MusitResult}
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ErrorHelper, ResourceHelper}
import play.api.libs.json.{JsObject, JsResult, Json}
import slick.dbio.DBIO

case class BaseEventDTO(id: Option[Long], links: Option[Seq[Link]], eventType: Int, note: Option[String])


case class BaseEventDTOHack(id: Option[Long], links: Option[Seq[Link]], eventType: String, note: Option[String]) {


}

object BaseEventDTOHack {
  def fromBaseEventDto(baseEvent: BaseEventDTO) = {
    val eventTypeName = (EventType.getById(baseEvent.eventType).get).name
    BaseEventDTOHack(baseEvent.id, baseEvent.links, eventTypeName, baseEvent.note)
  }
  implicit val format = Json.format[BaseEventDTOHack]
}

object BaseEventDTO {
  implicit val format = Json.format[BaseEventDTO]
}


class Event(eventType: EventType, dto: BaseEventDTO) {
  val id: Option[Long] = dto.id
  val note: Option[String] = dto.note
  val links: Option[Seq[Link]] = dto.links

  val baseEventDTO = dto

  /**
    * possible method to create an action to insert the extended event info.
    */
  // TODO: Should the function return DBIO[Unit] instead of DBIO[Long]? 
  def extendedInsertAction: Option[(Long) => DBIO[Int]] = None

  //Override this in subclasses
  def extendedToJson: Option[JsObject] = None

  def toJson = {
    val baseJson = Json.toJson(BaseEventDTOHack.fromBaseEventDto(baseEventDTO)).asInstanceOf[JsObject]
    extendedToJson.fold(baseJson)(extendedJson => baseJson ++ extendedJson)
  }

}

object Event {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Event] = {
    val baseEvent = fromJsonToBaseEvent(eventType, jsObject)
    baseEvent.map(dto => new Event(eventType, dto))
  }

  def fromJsonToBaseEvent(eventType: EventType, jsObject: JsObject): JsResult[BaseEventDTO] = {
    for {
      id <- (jsObject \ "id").validateOpt[Long]
      links <- (jsObject \ "links").validateOpt[Seq[Link]]
      note <- (jsObject \ "note").validateOpt[String]
    } yield BaseEventDTO(id, links, eventType.id, note)
  }

  def fromDatabase(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Event] = {
    MusitFuture.successful(new Event(eventType, baseEventDto))
  }

  def genericValidate(jsObject: JsObject): MusitResult[Event] = {

    val evtTypeName = (jsObject \ "eventType").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => eventType.makeEvent(jsObject) |> ResourceHelper.jsResultToMusitResult
    }
    maybeEventResult
  }
}

case class MoveDTO(to: Option[String])

object MoveDTO {
  implicit val format = Json.format[MoveDTO]
}

trait EventController {
  def fromJson(eventType: EventType, baseResult: JsResult[BaseEventDTO], jsObject: JsObject): JsResult[Event] = Event.fromJson(eventType, jsObject)

  def fromDatabase(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Event] = Event.fromDatabase(eventType, id, baseEventDto)
}

class Move(eventType: EventType, baseDTO: BaseEventDTO) extends Event(eventType, baseDTO)

object Move extends EventController


case class ControlDTO(blablabla: Option[String])

object ControlDTO {
  implicit val format = Json.format[ControlDTO]
}

class Control(eventType: EventType, baseDTO: BaseEventDTO, dto: ControlDTO) extends Event(eventType, baseDTO)

object Control extends EventController {
  override def fromJson(eventType: EventType, baseResult: JsResult[BaseEventDTO], jsObject: JsObject): JsResult[Control] = {
    for {
      baseDto <- baseResult
      controlEventDto <- jsObject.validate[ControlDTO]
    } yield new Control(eventType, baseDto, controlEventDto)
  }
}

