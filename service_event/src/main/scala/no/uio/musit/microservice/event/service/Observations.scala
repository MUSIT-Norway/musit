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

import no.uio.musit.microservice.event.dao.{ ObservationPestDao, ObservationFromToDao }
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{ JsObject, JsResult, Json }

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
abstract class ObservationFromTo(baseEventProps: BaseEventDto, val customDto: ObservationFromToDto) extends Event(baseEventProps) {
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
//
// ------------------------------------------------------------

class ObservationHypoxicAir(baseEventProps: BaseEventDto, customDto: ObservationFromToDto) extends ObservationFromTo(baseEventProps, customDto)

object ObservationHypoxicAirService extends ObservationFromToServiceBase with MultipleTablesNotUsingCustomFields {

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationHypoxicAir(baseProps, customDto.asInstanceOf[ObservationFromToDto])
}

// ------------------------------------------------------------
//  ObservationLightingCondition
// ------------------------------------------------------------

class ObservationLightingCondition(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val lightingCondition = this.getCustomOptString
}

object ObservationLightingConditionService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationLightingCondition(baseProps)

  def getCustomFieldsSpec = ObservationLightingConditionCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationCleaning
// ------------------------------------------------------------

class ObservationCleaning(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val cleaning = this.getCustomOptString
}

object ObservationCleaningService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationCleaning(baseProps)

  def getCustomFieldsSpec = ObservationCleaningCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationGas
// ------------------------------------------------------------

class ObservationGas(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val gas = this.getCustomOptString
}

object ObservationGasService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationGas(baseProps)

  def getCustomFieldsSpec = ObservationGasCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationMold
// ------------------------------------------------------------

class ObservationMold(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val mold = this.getCustomOptString
}

object ObservationMoldService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationMold(baseProps)

  def getCustomFieldsSpec = ObservationMoldCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationTheftProtection
// ------------------------------------------------------------

class ObservationTheftProtection(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val theftProtection = this.getCustomOptString
}

object ObservationTheftProtectionService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationTheftProtection(baseProps)

  def getCustomFieldsSpec = ObservationTheftProtectionCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationFireProtection
// ------------------------------------------------------------

class ObservationFireProtection(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val fireProtection = this.getCustomOptString
}

object ObservationFireProtectionService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationFireProtection(baseProps)

  def getCustomFieldsSpec = ObservationFireProtectionCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationPerimeterSecurity
// ------------------------------------------------------------

class ObservationPerimeterSecurity(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val perimeterSecurity = this.getCustomOptString
}

object ObservationPerimeterSecurityService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationPerimeterSecurity(baseProps)

  def getCustomFieldsSpec = ObservationPerimeterSecurityCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationWaterDamageAssessment
// ------------------------------------------------------------

class ObservationWaterDamageAssessment(baseEventProps: BaseEventDto) extends Event(baseEventProps) {
  val waterDamageAssessment = this.getCustomOptString
}

object ObservationWaterDamageAssessmentService extends SingleTableUsingCustomFields {
  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationWaterDamageAssessment(baseProps)

  def getCustomFieldsSpec = ObservationWaterDamageAssessmentCustomFieldsSpec.customFieldsSpec
}

// ------------------------------------------------------------
//  ObservationPest
// ------------------------------------------------------------

class ObservationPest(val baseProps: BaseEventDto, val dto: ObservationPestDto) extends Event(baseProps) {
  val identification = this.getCustomOptString
  val livssykluser = dto.lifeCycles
}

object ObservationPestService extends MultipleTablesAndUsingCustomFields {

  def getCustomFieldsSpec = ObservationPestCustomFieldsSpec.customFieldsSpec

  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event = new ObservationPest(baseProps, customDto.asInstanceOf[ObservationPestDto])

  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] = ObservationPestDao.getObservation(id)

  def createInsertCustomDtoAction(id: Long, event: Event) = {
    val specificEvent = event.asInstanceOf[ObservationPest]
    ObservationPestDao.insertAction(id, specificEvent.dto)
  }

  def validateCustomDto(jsObject: JsObject): JsResult[Dto] = jsObject.validate[ObservationPestDto]

  def customDtoToJson(event: Event): JsObject = Json.toJson(event.asInstanceOf[ObservationPest].dto).asInstanceOf[JsObject]
}

// ------------------------------------------------------------
//  ObservationAlcohol
// ------------------------------------------------------------

class ObservationAlcohol(val baseProps: BaseEventDto) extends Event(baseProps) {
  val condition = baseProps.getOptString
  val volume = baseProps.getOptDouble
}

object ObservationAlcoholService extends SingleTableUsingCustomFields {

  def getCustomFieldsSpec = ObservationAlcoholCustomFieldsSpec.customFieldsSpec

  def createEventInMemory(baseProps: BaseEventDto): Event = new ObservationAlcohol(baseProps)
}
