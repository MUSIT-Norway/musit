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
import play.api.libs.json.{JsObject, JsResult}
import slick.dbio.DBIO

case class BaseEvent(id: Option[Long], eventType: Int, links: Option[Seq[Link]], note: Option[String])

class Event(eventType: EventType, dto: BaseEvent) {
  val id: Option[Long] = dto.id
  val note: Option[String] = dto.note
  val links: Option[Seq[Link]] = dto.links
  def extendedInsertAction: Option[DBIO[Long]] = None
}

object Event {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Event] = ???
  def fromJsonToBaseEvent = ???
}

case class MoveDTO(to: Option[String])
class Move(eventType: EventType, baseDTO: BaseEvent, dto: MoveDTO) extends Event(eventType, baseDTO) {
  val to: Option[String] = dto.to
  override def extendedInsertAction = None
}
object Move {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Event] = Event.fromJson(eventType, jsObject)
}

case class ControlDTO()
class Control(eventType: EventType, baseDTO: BaseEvent, dto: ControlDTO) extends Event(eventType, baseDTO) {
  override def extendedInsertAction = None
}
object Control {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Control] = ???
}

case class ObservationDTO()
class Observation(eventType: EventType, baseDTO: BaseEvent, dto: ObservationDTO) extends Event(eventType, baseDTO) {
  override def extendedInsertAction = None
}
object Observation {
  def fromJson(eventType: EventType, jsObject: JsObject): JsResult[Observation] = ???
}