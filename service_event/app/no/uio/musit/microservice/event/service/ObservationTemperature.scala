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

import no.uio.musit.microservice.event.dao.{ ObservationTemperatureDao, ObservationTemperatureDto }
import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservice.event.domain.{ BaseEventProps, Dto, Event, MultipleTablesMultipleDtos }
import play.api.libs.json.{ JsObject, JsResult, Json }

import scala.concurrent.Future

/**
 * Created by jstabel on 7/5/16.
 */

class ObservationTemperature(val baseProps: BaseEventProps, val customDto: ObservationTemperatureDto) extends Event(baseProps) {
  val temperatureFrom = customDto.temperatureFrom
  val temperatureTo = customDto.temperatureTo
}

object ObservationTemperatureService extends MultipleTablesMultipleDtos {

  def createEventInMemory(baseProps: BaseEventProps, customDto: Dto): Event = new ObservationTemperature(baseProps, customDto.asInstanceOf[ObservationTemperatureDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventProps): Future[Option[Dto]] = ObservationTemperatureDao.getObservation(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[ObservationTemperature]
    ObservationTemperatureDao.insertAction(specificEvent.customDto.copy(id = Some(id)))
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[ObservationTemperatureDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[ObservationTemperature].customDto).asInstanceOf[JsObject]
}

