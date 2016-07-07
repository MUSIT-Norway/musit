
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

package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{JsObject, JsResult, Json}

import scala.concurrent.Future

/**
 * Created by jstabel on 7/5/16.
 */

/*
trait ControlSpecificDtoBase extends DTO {
  ok: Boolean
}
*/

/** "Abstract" base class for specific events */
class ControlSpecific(baseEventProps: BaseEventProps, val customDto: ControlSpecificDto) extends Event(baseEventProps) {

  val ok = customDto.ok
}

/** "Abstract" base class for specific control event implementations */
class ControlSpecificService {

  def baseTableToCustomDto(baseEventDto: BaseEventDto): Dto = ControlSpecificDto(baseEventDto.getBool)

  def customDtoToBaseTable(event: Event, baseEventDto: BaseEventDto): BaseEventDto = {
    val thisEvent = event.asInstanceOf[ControlSpecific]
    baseEventDto.setBool(thisEvent.customDto.ok)
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[ControlSpecificDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[ControlSpecific].customDto).asInstanceOf[JsObject]
}

// --- ControlTemperature

class ControlTemperature(baseEventProps: BaseEventProps, customDto: ControlSpecificDto) extends ControlSpecific(baseEventProps, customDto)

object ControlTemperatureService extends ControlSpecificService with SingleTableMultipleDtos {
  def createEventInMemory(baseProps: BaseEventProps, customDto: Dto): Event = new ControlTemperature(baseProps, customDto.asInstanceOf[ControlSpecificDto])
}

// --- ControlAir

class ControlAir(baseEventProps: BaseEventProps, customDto: ControlSpecificDto) extends ControlSpecific(baseEventProps, customDto)

object ControlAirService extends ControlSpecificService with SingleTableMultipleDtos {
  def createEventInMemory(baseProps: BaseEventProps, customDto: Dto): Event = new ControlAir(baseProps, customDto.asInstanceOf[ControlSpecificDto])
}
