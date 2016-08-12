/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dto.BaseEventDto
import no.uio.musit.microservice.event.service.CustomFieldsSpec

/** "Abstract" base type for specific events */
sealed trait ControlSpecific { self: Event =>

  val baseEventDto: BaseEventDto
  val customFieldsSpec = CustomFieldsSpec().defineRequiredBoolean("ok")
  val ok: Boolean = self.getCustomBool

}

case class ControlTemperature(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlInertluft(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlRelativLuftfuktighet(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlLysforhold(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlRenhold(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlGass(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlMugg(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlSkadedyr(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

case class ControlSprit(baseEventDto: BaseEventDto) extends Event(baseEventDto) with ControlSpecific

