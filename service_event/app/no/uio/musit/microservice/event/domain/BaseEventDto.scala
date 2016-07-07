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

import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

/**
  * Created by jstabel on 7/7/16.
  */

object BaseEventDto {
  //def fromBaseEventDto(eventDto: BaseEventDto, relatedSubEvents: Seq[RelatedEvents]) = BaseEventProps(eventDto.id, eventDto.links, eventDto.eventType, eventDto.note, relatedSubEvents)

  //#OLD  implicit object baseEventPropsWrites extends Writes[BaseEventProps] {
  implicit object baseEventPropsWrites extends Writes[BaseEventDto] {

    // TODO: Fix this, this currently writes "note": null if no note!
    def writes(a: BaseEventDto): JsValue = {
      Json.obj(
        "id" -> a.id,
        "links" -> a.links,
        "eventType" -> a.eventType,
        "note" -> a.note
      )
    }
  }
}

case class BaseEventDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String], relatedSubEvents: Seq[RelatedEvents], partOf: Option[Long], valueLong: Option[Long], valueString: Option[String])  {
  /*#OLD
    /** Copies all data except custom event data over to the baseEventDto object */
    def toBaseEventDto(parentId: Option[Long]) = BaseEventDto(this.id, this.links, this.eventType, this.note, parentId, None, None)
*/
    def toJson: JsObject = Json.toJson(this).asInstanceOf[JsObject]



/*
case class BaseEventDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String],
                        partOf: Option[Long], valueLong: Option[Long], valueString: Option[String]) {
                        */

  def getOptBool = valueLong match {
    case Some(1) => Some(true)
    case Some(0) => Some(false)
    case None => None
    case n => throw new MusitInternalErrorException(s"Wrong boolean value $n")
  }

  def getBool = getOptBool match {
    case Some(b) => b
    case None => throw new MusitInternalErrorException("Missing boolean value")
  }

  private def boolToLong(bool: Boolean) = if (bool) 1 else 0

  def setBool(value: Boolean) = this.copy(valueLong = Some(boolToLong(value)))

  def setString(value: String) = this.copy(valueString = Some(value))

  def setOptionString(value: Option[String]) = {
    value match {
      case Some(s) => setString(s)
      case None => this.copy(valueString = None)
    }
  }


  //#OLD def props(relatedSubEvents: Seq[RelatedEvents]) = BaseEventProps.fromBaseEventDto(this, relatedSubEvents)
}
