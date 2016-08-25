
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

import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{ JsObject, JsResult, Json }

import scala.concurrent.Future

/**
 * Created by jstabel on 7/5/16.
 */

/** "Abstract" base class for specific events */
class ControlSpecific(baseEventProps: BaseEventDto) extends Event(baseEventProps) {

  val ok = getCustomBool
}

/** "Abstract" base class for specific control event implementations */
class ControlSpecificService {
  def getCustomFieldsSpec = ControlSpecificDtoSpec.customFieldsSpec
}

// --- ControlTemperature

class ControlTemperature(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlTemperatureService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlTemperature(baseProps)
}

// --- ControlHypoxicAir

class ControlHypoxicAir(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlHypoxicAirService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlHypoxicAir(baseProps)
}

// --- ControlRelativeHumidity

class ControlRelativeHumidity(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlRelativeHumidityService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlRelativeHumidity(baseProps)
}

// --- ControlLightingCondition

class ControlLightingCondition(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlLightingConditionService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlLightingCondition(baseProps)
}

// --- ControlCleaning

class ControlCleaning(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlCleaningService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlCleaning(baseProps)
}

// --- ControlGas

class ControlGas(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlGasService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlGas(baseProps)
}

// --- ControlMold

class ControlMold(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlMoldService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlMold(baseProps)
}

// --- ControlPest

class ControlPest(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlPestService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlPest(baseProps)
}

// --- ControlAlcohol

class ControlAlcohol(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlAlcoholService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlAlcohol(baseProps)
}
