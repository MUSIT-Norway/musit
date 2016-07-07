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
import play.api.libs.json._

/**
  * Created by jstabel on 7/7/16.
  */

object BaseEventDto {
  implicit object baseEventPropsWrites extends Writes[BaseEventDto] {

    // TODO: Fix this, this currently writes "note": null if no note!
    def writes(baseEventDto: BaseEventDto): JsValue = {
      var jsObj = Json.obj(
        "id" -> baseEventDto.id,
        "links" -> baseEventDto.links,
        "eventType" -> baseEventDto.eventType
      )
      baseEventDto.note.foreach(note=> {
        jsObj = jsObj.+("note"->JsString(note))
      })
      CustomFieldsHandler.writeCustomFieldsToJsonIfAny(baseEventDto, jsObj)
    }
  }
}

case class BaseEventDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String], relatedSubEvents: Seq[RelatedEvents], partOf: Option[Long], valueLong: Option[Long], valueString: Option[String])  {
    def toJson: JsObject = Json.toJson(this).asInstanceOf[JsObject]

  def getOptBool = valueLong match {
    case Some(1) => Some(true)
    case Some(0) => Some(false)
    case None => None
    case n => throw new MusitInternalErrorException(s"Wrong boolean value $n on base event")
  }

  def getBool = getOptBool match {
    case Some(b) => b
    case None => throw new MusitInternalErrorException("Missing boolean value onj base event")
  }

  def getInteger = valueLong match {
    case Some(n) => n
    case None => throw new MusitInternalErrorException("Missing integer value on base event")

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

  def hasValueLong = valueLong.isDefined
  def hasValueString = valueString.isDefined

  def getOptString: Option[String] = valueString
  def getString: String = getOptString.getOrElse("")
}
