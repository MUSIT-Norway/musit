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

import no.uio.musit.microservice.event.dao.{ObservationFromToDao, ObservationSkadedyrDao}
import no.uio.musit.microservice.event.domain._
import no.uio.musit.microservice.event.dto.{BaseEventDto, Dto, ObservationFromToDto, ObservationSkadedyrDto}
import play.api.libs.json.{JsObject, JsResult, Json}

import scala.concurrent.Future

// ------------------------------------------------------------
//  Observation (generic container for subobservations)
// ------------------------------------------------------------

class Observation(baseProps: BaseEventDto) extends Event(baseProps) {
  val subObservations = this.getAllSubEventsAs[Observation] //TEMP!   //Can be subtyped to SpecificObservation when that has been created
}

object ObservationService extends SingleTableNotUsingCustomFields {
  def createEventInMemory(baseEventProps: BaseEventDto): Event = {
    new Observation(baseEventProps)
  }
}

// ----------------------

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

  def getCustomFieldsSpec = ObservationLysCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationRenhold
// ------------------------------------------------------------

class ObservationRenhold(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val renhold = this.getCustomOptString
}

object ObservationRenholdService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationRenhold(baseProps)

  def getCustomFieldsSpec = ObservationRenholdCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationGass
// ------------------------------------------------------------

class ObservationGass(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val gass = this.getCustomOptString
}

object ObservationGassService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationGass(baseProps)

  def getCustomFieldsSpec = ObservationGassCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationMugg
// ------------------------------------------------------------

class ObservationMugg(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val mugg = this.getCustomOptString
}

object ObservationMuggService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationMugg(baseProps)

  def getCustomFieldsSpec = ObservationMuggCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationTyveriSikring
// ------------------------------------------------------------

class ObservationTyveriSikring(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val tyveriSikring = this.getCustomOptString
}

object ObservationTyveriSikringService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationTyveriSikring(baseProps)

  def getCustomFieldsSpec = ObservationTyveriSikringCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationBrannSikring
// ------------------------------------------------------------

class ObservationBrannSikring(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val brannSikring = this.getCustomOptString
}

object ObservationBrannSikringService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationBrannSikring(baseProps)

  def getCustomFieldsSpec = ObservationBrannSikringCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationSkallSikring
// ------------------------------------------------------------

class ObservationSkallSikring(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val skallSikring = this.getCustomOptString
}

object ObservationSkallSikringService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationSkallSikring(baseProps)

  def getCustomFieldsSpec = ObservationSkallSikringCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationVannskadeRisiko
// ------------------------------------------------------------

class ObservationVannskadeRisiko(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val vannskadeRisiko = this.getCustomOptString
}

object ObservationVannskadeRisikoService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationVannskadeRisiko(baseProps)

  def getCustomFieldsSpec = ObservationVannskadeRisikoCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationSkadedyr
// ------------------------------------------------------------

class ObservationSkadedyr(val baseProps: BaseEventDto, val dto: ObservationSkadedyrDto) extends Event(baseProps) {
  val identifikasjon = this.getCustomOptString
  val livssykluser = dto.livssykluser
}

object ObservationSkadedyrService extends MultipleTablesAndUsingCustomFields {

  def getCustomFieldsSpec = ObservationSkadedyrCustomFieldsSpec.customFieldsSpec

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationSkadedyr(baseProps, customDto.asInstanceOf[ObservationSkadedyrDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] = ObservationSkadedyrDao.getObservation(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[ObservationSkadedyr]
    ObservationSkadedyrDao.insertAction(id, specificEvent.dto)
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[ObservationSkadedyrDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[ObservationSkadedyr].dto).asInstanceOf[JsObject]
}

// ------------------------------------------------------------
//  ObservationSprit
// ------------------------------------------------------------

class ObservationSprit(val baseProps: BaseEventDto) extends Event(baseProps) {
  val tilstand = baseProps.getOptString
  val volum = baseProps.getOptDouble
}

object ObservationSpritService extends SingleTableUsingCustomFields {
  def getCustomFieldsSpec = ObservationSpritCustomFieldsSpec.customFieldsSpec


  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationSprit(baseProps)
}
