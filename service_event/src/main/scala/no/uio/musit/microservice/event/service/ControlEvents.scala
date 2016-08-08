
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

// --- ControlInertluft

class ControlInertluft(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlInertluftService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlInertluft(baseProps)
}

// --- ControlRelativLuftfuktighet

class ControlRelativLuftfuktighet(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlRelativLuftfuktighetService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlRelativLuftfuktighet(baseProps)
}

// --- ControlLysforhold

class ControlLysforhold(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlLysforholdService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlLysforhold(baseProps)
}

// --- ControlRenhold

class ControlRenhold(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlRenholdService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlRenhold(baseProps)
}

// --- ControlGass

class ControlGass(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlGassService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlGass(baseProps)
}

// --- ControlMugg

class ControlMugg(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlMuggService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlMugg(baseProps)
}

// --- ControlSkadedyr

class ControlSkadedyr(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlSkadedyrService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlSkadedyr(baseProps)
}

// --- ControlSprit

class ControlSprit(baseEventProps: BaseEventDto) extends ControlSpecific(baseEventProps)

object ControlSpritService extends ControlSpecificService with SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ControlSprit(baseProps)
}
