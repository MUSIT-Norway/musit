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

import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ JsObject, JsResult, Json }
import slick.dbio.DBIO

case class BaseEventDTO(id: Option[Long], links: Option[Seq[Link]], eventType: Int, note: Option[String])

object BaseEventDTO {

  //  def tupled = (BaseEventDTO.apply _).tupled

  implicit val format = Json.format[BaseEventDTO]
}

class Event(eventType: EventType, dto: BaseEventDTO) {
  val id: Option[Long] = dto.id
  val note: Option[String] = dto.note
  val links: Option[Seq[Link]] = dto.links

  val baseEventDTO = dto
  /**
   * possible method to create an action to insert the extended event info.
   *
   * @return
   */
  // TODO: Should the function return DBIO[Unit] instead of DBIO[Long]? 
  def extendedInsertAction: Option[Long => DBIO[Long]] = None

  //Override this in subclasses
  def extendedToJson: Option[JsObject] = None

  def toJson = {
    val baseJson = Json.toJson(baseEventDTO).asInstanceOf[JsObject]
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
}

case class MoveDTO(to: Option[String])

object MoveDTO {

  //  def tupled = (MoveDTO.apply _).tupled

  implicit val format = Json.format[MoveDTO]
}

class Move(eventType: EventType, baseDTO: BaseEventDTO, dto: MoveDTO) extends Event(eventType, baseDTO) {
  val to: Option[String] = dto.to

  override def extendedInsertAction = None

  override def extendedToJson: Option[JsObject] = Some(Json.toJson(dto).asInstanceOf[JsObject])

}

object Move {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Move] = {
    for {
      baseEventDto <- Event.fromJsonToBaseEvent(eventType, jsObject)
      moveEventDto <- jsObject.validate[MoveDTO]
    } yield new Move(eventType, baseEventDto, moveEventDto)
  }
}

case class ControlDTO(blablabla: Option[String])

object ControlDTO {

  //def tupled = (ControlDTO.apply _).tupled

  implicit val format = Json.format[ControlDTO]
}

class Control(eventType: EventType, baseDTO: BaseEventDTO, dto: ControlDTO) extends Event(eventType, baseDTO) {
  override def extendedInsertAction = None
}

object Control {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Control] = {
    for {
      baseEventDto <- Event.fromJsonToBaseEvent(eventType, jsObject)
      controlEventDto <- jsObject.validate[ControlDTO]
    } yield new Control(eventType, baseEventDto, controlEventDto)
  }
}

case class ObservationDTO(blablabla: Option[String])

object ObservationDTO {

  //def tupled = (ObservationDTO.apply _).tupled

  implicit val format = Json.format[ObservationDTO]
}

class Observation(eventType: EventType, baseDTO: BaseEventDTO, dto: ObservationDTO) extends Event(eventType, baseDTO) {
  override def extendedInsertAction = None
}

object Observation {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Observation] = {
    for {
      baseEventDto <- Event.fromJsonToBaseEvent(eventType, jsObject)
      observationDto <- jsObject.validate[ObservationDTO]
    } yield new Observation(eventType, baseEventDto, observationDto)
  }
}