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

import no.uio.musit.microservice.event.dto.{BaseEventDto, ObservationFromToDto, ObservationSkadedyrDto}
import no.uio.musit.microservice.event.service.CustomFieldsSpec

case class Observation(baseProps: BaseEventDto) extends Event(baseProps) {
  //TEMP!   Can be subtyped to SpecificObservation when that has been created
  val subObservations = this.getAllSubEventsAs[Observation]
}

/** "Abstract" base class for specific to-from observations */
sealed trait ObservationFromTo {

  val baseEventDto: BaseEventDto
  val customDto: ObservationFromToDto

  val from = customDto.from
  val to = customDto.to
}

case class ObservationRelativeHumidity(
  baseEventDto: BaseEventDto,
  customDto: ObservationFromToDto
) extends Event(baseEventDto) with ObservationFromTo

case class ObservationTemperature(
  baseEventDto: BaseEventDto,
  customDto: ObservationFromToDto
) extends Event(baseEventDto) with ObservationFromTo


case class ObservationInertAir(
  baseEventDto: BaseEventDto,
  customDto: ObservationFromToDto
) extends Event(baseEventDto) with ObservationFromTo


case class ObservationLys(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val lysforhold = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("lysforhold")
}

case class ObservationRenhold(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val renhold = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("renhold")
}

case class ObservationGass(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val gass = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("gass")
}

case class ObservationMugg(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val mugg = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("mugg")
}

case class ObservationTyveriSikring(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val tyveriSikring = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("tyverisikring")
}

case class ObservationBrannSikring(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val brannSikring = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("brannsikring")
}

case class ObservationSkallSikring(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val skallSikring = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("skallsikring")
}

case class ObservationVannskadeRisiko(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val vannskadeRisiko = getCustomOptString
  val customFieldsSpec = CustomFieldsSpec().defineOptString("vannskaderisiko")
}

case class ObservationSkadedyr(
  baseEventDto: BaseEventDto,
  dto: ObservationSkadedyrDto
) extends Event(baseEventDto) with HasCustomField {
  val identifikasjon = getCustomOptString
  val livssykluser = dto.livssykluser
  val customFieldsSpec = CustomFieldsSpec().defineOptString("identifikasjon")
}

case class ObservationSprit(
  baseEventDto: BaseEventDto
) extends Event(baseEventDto) with HasCustomField {
  val tilstand = baseEventDto.getOptString
  val volum = baseEventDto.getOptDouble
  val customFieldsSpec = CustomFieldsSpec().defineOptString("tilstand").defineOptDouble("volum")
}