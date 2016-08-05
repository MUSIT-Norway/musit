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

import no.uio.musit.microservice.event.domain.{ BaseEventDto, Event, EventType }
import no.uio.musit.microservice.event.service.Validators.{ DoubleValidator, IntegerValidator, StringValidator }
import play.api.libs.json._

/**
 * A system for defining usage of the custom values in the base event table.
 * We have valueLong and valueString in the base event table, event implementations can us these for storing custom values.
 * (This often saves us from having to create an extra table in the database for a given event type.)
 *
 * In the valueLong field, we support storing either a long/integer or a boolean
 * In the valueString field, we currently only support storing a string.
 * (We could of course also store a boolean or int or whatever in valueString, but then we would need to differentiate
 * between say a boolean in the valueLong and boolean in valueString. If an event-type needs this, it probably is "complex enough" that it should create it's own separate table.)
 *
 * @author jstabel
 */

object Validators {
  type IntegerValidator = Int => Option[String] //Not implemented yet. Should probably return a Success/Failure thing.
  type StringValidator = String => Option[String] //Not implemented yet. Should probably return a Success/Failure thing.
  type DoubleValidator = Double => Option[String] //Not implemented yet. Should probably return a Success/Failure thing.
}

sealed trait ValueLongField

case class BooleanField(name: String, required: Boolean) extends ValueLongField

case class IntegerField(name: String, required: Boolean, validator: Option[IntegerValidator] = None) extends ValueLongField

case class ValueStringField(name: String, required: Boolean, validator: Option[StringValidator] = None)

case class ValueDoubleField(name: String, required: Boolean, validator: Option[DoubleValidator] = None)

object CustomValuesInEventTable {

  def getBool(event: Event) = event.baseEventProps.getBool

  def getOptBool(event: Event) = event.baseEventProps.getOptBool

  def getString(event: Event) = event.baseEventProps.getString

  def getOptString(event: Event) = event.baseEventProps.getOptString

  def getDouble(event: Event) = event.baseEventProps.getDouble

  def getOptDouble(event: Event) = event.baseEventProps.getOptDouble

}

object CustomFieldsHandler {
  def getOptFieldsSpec(eventType: EventType): Option[CustomFieldsSpec] = {
    eventType.eventImplementation match {
      case s: UsingCustomFieldsInBaseEventTable => Some(s.getCustomFieldsSpec)
      case _ => None
    }
  }

  def getOptFieldsSpec(baseEventDto: BaseEventDto): Option[CustomFieldsSpec] = {
    getOptFieldsSpec(baseEventDto.eventType)
  }

  def writeCustomFieldsToJsonIfAny(baseEventDto: BaseEventDto, jsObject: JsObject): JsObject = {
    getOptFieldsSpec(baseEventDto).fold(jsObject) { fieldsSpec =>

      var resultJsObject = baseEventDto.valueLong.fold(jsObject) { myValueLong =>
        {
          fieldsSpec.intValueHandler.fold(jsObject) {
            case BooleanField(name, req) =>
              jsObject.+(name -> JsBoolean(baseEventDto.getOptBool.get))

            case IntegerField(name, _, _) =>
              jsObject.+(name -> JsNumber(baseEventDto.valueLong.get))
          }
        }
      }
      resultJsObject = baseEventDto.valueString.fold(resultJsObject) { myValueString =>
        {
          fieldsSpec.stringValueHandler.fold(resultJsObject) {
            valueStringFieldSpec =>
              resultJsObject + (valueStringFieldSpec.name -> JsString(myValueString))
          }
        }
      }
      resultJsObject = baseEventDto.valueDouble.fold(resultJsObject) { myValueDouble =>
        {
          fieldsSpec.doubleValueHandler.fold(resultJsObject) {
            valueDoubleFieldSpec =>
              resultJsObject + (valueDoubleFieldSpec.name -> JsNumber(myValueDouble))
          }
        }
      }
      resultJsObject
    }
  }

  def validateCustomIntegerFieldFromJsonIfAny(eventType: EventType, jsObject: JsObject): JsResult[Option[Long]] = {
    getOptFieldsSpec(eventType).fold[JsResult[Option[Long]]](JsSuccess[Option[Long]](None)) { fieldsSpec =>
      fieldsSpec.intValueHandler.fold[JsResult[Option[Long]]](JsSuccess[Option[Long]](None)) {
        case BooleanField(name, req) =>
          if (req)
            (jsObject \ name).validate[Boolean].map(b => if (b) Some(1L) else Some(0L))
          else
            (jsObject \ name).validateOpt[Boolean].map(optB => optB.map(b => if (b) 1L else 0L))
        case IntegerField(name, req, _) =>
          if (req)
            (jsObject \ name).validate[Long].map(Some(_))
          else
            (jsObject \ name).validateOpt[Long]

      }
    }
  }

  def validateCustomStringFieldFromJsonIfAny(eventType: EventType, jsObject: JsObject): JsResult[Option[String]] = {
    getOptFieldsSpec(eventType).fold[JsResult[Option[String]]](JsSuccess[Option[String]](None)) { fieldsSpec =>
      fieldsSpec.stringValueHandler.fold[JsResult[Option[String]]](JsSuccess[Option[String]](None)) {
        valueStringFieldSpec =>
          if (valueStringFieldSpec.required)
            (jsObject \ valueStringFieldSpec.name).validate[String].map(Some(_))
          else
            (jsObject \ valueStringFieldSpec.name).validateOpt[String]

      }
    }
  }

  def validateCustomDoubleFieldFromJsonIfAny(eventType: EventType, jsObject: JsObject): JsResult[Option[Double]] = {
    getOptFieldsSpec(eventType).fold[JsResult[Option[Double]]](JsSuccess[Option[Double]](None)) { fieldsSpec =>
      fieldsSpec.doubleValueHandler.fold[JsResult[Option[Double]]](JsSuccess[Option[Double]](None)) {
        valueDoubleFieldSpec =>
          if (valueDoubleFieldSpec.required)
            (jsObject \ valueDoubleFieldSpec.name).validate[Double].map(Some(_))
          else
            (jsObject \ valueDoubleFieldSpec.name).validateOpt[Double]
      }
    }
  }

}

case class CustomFieldsSpec(
    intValueHandler: Option[ValueLongField] = None,
    stringValueHandler: Option[ValueStringField] = None,
    doubleValueHandler: Option[ValueDoubleField] = None
) {

  //Just a precondition, check that the programmer aren't trying to define to different usages of the same valueLong-field in the event-table!
  def assertNotAlreadyDefinedIntValueHandler(name: String) = {
    assert(!intValueHandler.isDefined, s"Assert failed, custom integer field already in use, when trying to define property with name $name")

  }

  //Just a precondition, check that the programmer aren't trying to define to different usages of the same valueString-field in the event-table!
  def assertNotAlreadyDefinedStringValueHandler(name: String) = {
    assert(!stringValueHandler.isDefined, s"Assert failed, custom string field already in use, when trying to define property with name $name")
  }

  //Just a precondition, check that the programmer aren't trying to define to different usages of the same valueDouble-field in the event-table!
  def assertNotAlreadyDefinedDoubleValueHandler(name: String) = {
    assert(!doubleValueHandler.isDefined, s"Assert failed, custom double field already in use, when trying to define property with name $name")
  }

  def defineRequiredBoolean(name: String) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(BooleanField(name, true)))
  }

  def defineRequiredInt(name: String) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(IntegerField(name, true, None)))
  }

  def defineRequiredInt(name: String, validator: IntegerValidator) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(IntegerField(name, true, Some(validator))))
  }

  def defineRequiredString(name: String) = {
    assertNotAlreadyDefinedStringValueHandler(name)
    this.copy(stringValueHandler = Some(ValueStringField(name, true, None)))
  }

  def defineRequiredString(name: String, validator: StringValidator) = {
    assertNotAlreadyDefinedStringValueHandler(name)
    this.copy(stringValueHandler = Some(ValueStringField(name, true, Some(validator))))
  }

  def defineRequiredDouble(name: String) = {
    assertNotAlreadyDefinedDoubleValueHandler(name)
    this.copy(doubleValueHandler = Some(ValueDoubleField(name, true, None)))
  }

  def defineOptDouble(name: String) = {
    assertNotAlreadyDefinedDoubleValueHandler(name)
    this.copy(doubleValueHandler = Some(ValueDoubleField(name, false, None)))
  }

  def defineOptBoolean(name: String) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(BooleanField(name, false)))
  }

  def defineOptInt(name: String) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(IntegerField(name, false, None)))
  }

  def defineOptInt(name: String, validator: IntegerValidator) = {
    assertNotAlreadyDefinedIntValueHandler(name)
    this.copy(intValueHandler = Some(IntegerField(name, false, Some(validator))))
  }

  def defineOptString(name: String) = {
    assertNotAlreadyDefinedStringValueHandler(name)
    this.copy(stringValueHandler = Some(ValueStringField(name, false, None)))
  }

  def defineOptString(name: String, validator: StringValidator) = {
    assertNotAlreadyDefinedStringValueHandler(name)
    this.copy(stringValueHandler = Some(ValueStringField(name, false, Some(validator))))
  }
}

