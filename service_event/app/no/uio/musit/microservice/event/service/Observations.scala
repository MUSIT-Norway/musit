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

import no.uio.musit.microservice.event.dao.{EnvRequirementDao, ObservationFromToDao}
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{JsObject, JsResult, Json}

import scala.concurrent.Future

/** "Abstract" base class for specific to-from observations */
class ObservationFromTo(baseEventProps: BaseEventDto, val customDto: ObservationFromToDto) extends Event(baseEventProps) {

  val from = customDto.from
  val to = customDto.to
}

/** "Abstract" base class for specific observationFromTo implementations */


class ObservationFromToServiceBase {


  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] = ObservationFromToDao.getObservationFromTo(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[ObservationFromTo]
    ObservationFromToDao.insertAction(specificEvent.customDto.copy(id = Some(id)))
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[ObservationFromToDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[ObservationFromTo].customDto).asInstanceOf[JsObject]
}

// ----------------------

class ObservationRelativeHumidity(baseEventProps: BaseEventDto, customDto: ObservationFromToDto) extends ObservationFromTo(baseEventProps, customDto)


object ObservationRelativeHumidityService extends ObservationFromToServiceBase with MultipleTablesNotUsingCustomFields {

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationRelativeHumidity(baseProps, customDto.asInstanceOf[ObservationFromToDto])
}

// ------------------------------------------------------------
//  ObservationTemperature
// ------------------------------------------------------------

class ObservationTemperature(baseEventProps: BaseEventDto, customDto: ObservationFromToDto) extends ObservationFromTo(baseEventProps, customDto)

object ObservationTemperatureService extends ObservationFromToServiceBase with MultipleTablesNotUsingCustomFields {

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationTemperature(baseProps, customDto.asInstanceOf[ObservationFromToDto])
}


// ------------------------------------------------------------
//  ObservationInertAir
// ------------------------------------------------------------

class ObservationInertAir(baseEventProps: BaseEventDto, customDto: ObservationFromToDto) extends ObservationFromTo(baseEventProps, customDto)

object ObservationInertAirService extends ObservationFromToServiceBase with MultipleTablesNotUsingCustomFields {

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationInertAir(baseProps, customDto.asInstanceOf[ObservationFromToDto])
}


// ------------------------------------------------------------
//  ObservationLys
// ------------------------------------------------------------
class ObservationLys(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val lysforhold = this.getCustomOptString
}

object ObservationLysService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationLys(baseProps)

  def getCustomFieldsSpec = ObservationLysDtoSpec.customFieldsSpec
}

