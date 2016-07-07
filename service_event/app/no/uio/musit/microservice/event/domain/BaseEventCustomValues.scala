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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.domain.Validators.{IntegerValidator, StringValidator}
import no.uio.musit.microservice.event.service.UsingCustomFieldsInBaseEventTable
import play.api.libs.json._

/**
  * Created by jstabel on 7/7/16.
  */


object Validators {
  type IntegerValidator = Int => Option[String]
  type StringValidator = Int => Option[String]


}

sealed trait ValueLongField

case class BooleanField(name: String, required: Boolean) extends ValueLongField

case class IntegerField(name: String, required: Boolean, validator: Option[IntegerValidator] = None) extends ValueLongField


case class ValueStringField(name: String, required: Boolean, validator: Option[IntegerValidator] = None)



object CustomValuesInEventTable {

  def getBool(event: Event) = event.baseEventProps.getBool
  def getOptBool(event: Event) = event.baseEventProps.getOptBool


  //Empty string if none.
  def getString(event: Event) = event.baseEventProps.getString

  def getOptString(event: Event) = event.baseEventProps.getOptString

  //def setBool(event: Event, value: Boolean)

}

object CustomFieldsHandler {
  def getOptFieldsSpec(eventType: EventType): Option[CustomEventFieldsSpec] = {
    eventType.eventImplementation match {
      case s: UsingCustomFieldsInBaseEventTable => Some(s.getCustomFieldsSpec)
      case _ => None
    }
  }

  def getOptFieldsSpec(baseEventDto: BaseEventDto): Option[CustomEventFieldsSpec] = {
    getOptFieldsSpec(baseEventDto.eventType)
  }

  def writeCustomFieldsToJsonIfAny(baseEventDto: BaseEventDto, jsObject: JsObject): JsObject = {
    getOptFieldsSpec(baseEventDto).fold(jsObject) { fieldsSpec =>

      var resultJsObject = baseEventDto.valueLong.fold(jsObject) { myValueLong => {
        fieldsSpec.intValueHandler.fold(jsObject) {
          valueLongFieldSpec => valueLongFieldSpec
          match {
            case BooleanField(name, req) =>
              //println(s"Adding boolean field: $name value: ${baseEventDto.getBool}")
              jsObject.+(name -> JsBoolean(baseEventDto.getBool))

            case IntegerField(name, _, _) =>
              //println(s"Adding integer field: $name value: ${baseEventDto.getInteger}")
              jsObject.+(name -> JsNumber(baseEventDto.getInteger))
          }
        }
      }
      }
      resultJsObject = baseEventDto.valueString.fold(resultJsObject) { myValueString => {
        fieldsSpec.stringValueHandler.fold(resultJsObject) {
          valueStringFieldSpec =>
            //println(s"Adding string field: ${valueStringFieldSpec.name } value: ${myValueString}")
            resultJsObject.+(valueStringFieldSpec.name -> JsString(myValueString))
        }
      }
      }
      resultJsObject
    }
  }

  val jsResultNoneOfLong: JsResult[Option[Long]] = JsSuccess[Option[Long]](None)
  val jsResultNoneOfString: JsResult[Option[String]] = JsSuccess[Option[String]](None)

  def validateCustomIntegerFieldFromJsonIfAny(eventType: EventType, jsObject: JsObject): JsResult[Option[Long]] = {
    getOptFieldsSpec(eventType).fold(jsResultNoneOfLong) { fieldsSpec =>
      fieldsSpec.intValueHandler.fold(jsResultNoneOfLong) {
        valueLongFieldSpec => valueLongFieldSpec
        match {
          case BooleanField(name, req) =>
            if (req)
              (jsObject \ name).validate[Boolean].map(b=> if(b) Some(1L) else Some(0L))
            else
              (jsObject \ name).validateOpt[Boolean].map(optB=> optB.map(b=> if(b) 1L else 0L))
          case IntegerField(name, req, _) =>
            if (req)
              (jsObject \ name).validate[Long].map(Some(_))
            else
              (jsObject \ name).validateOpt[Long]

        }
      }
    }
  }


  def validateCustomStringFieldFromJsonIfAny(eventType: EventType, jsObject: JsObject): JsResult[Option[String]] = {
    getOptFieldsSpec(eventType).fold(jsResultNoneOfString) { fieldsSpec =>
      fieldsSpec.stringValueHandler.fold(jsResultNoneOfString) {
        valueStringFieldSpec =>
            if (valueStringFieldSpec.required)
              (jsObject \ valueStringFieldSpec.name).validate[String].map(Some(_))
            else
              (jsObject \ valueStringFieldSpec.name).validateOpt[String]

        }
      }
  }
}


// TODO: Add checks that we don't redefine anything!
case class CustomEventFieldsSpec(intValueHandler: Option[ValueLongField] = None, stringValueHandler: Option[ValueStringField] = None) {
  def defineRequiredBoolean(name: String) = this.copy(intValueHandler = Some(BooleanField(name, true)))

  def defineRequiredInt(name: String) = this.copy(intValueHandler = Some(IntegerField(name, true, None)))

  def defineRequiredInt(name: String, validator: IntegerValidator) = this.copy(intValueHandler = Some(IntegerField(name, true, Some(validator))))

  def defineRequiredString(name: String) = this.copy(stringValueHandler = Some(ValueStringField(name, true, None)))

  def defineRequiredString(name: String, validator: StringValidator) = this.copy(stringValueHandler = Some(ValueStringField(name, true, Some(validator))))

  def defineOptBoolean(name: String) = this.copy(intValueHandler = Some(BooleanField(name, false)))

  def defineOptInt(name: String) = this.copy(intValueHandler = Some(IntegerField(name, false, None)))

  def defineOptInt(name: String, validator: IntegerValidator) = this.copy(intValueHandler = Some(IntegerField(name, false, Some(validator))))

  def defineOptString(name: String) = this.copy(stringValueHandler = Some(ValueStringField(name, false, None)))

  def defineOptString(name: String, validator: StringValidator) = this.copy(stringValueHandler = Some(ValueStringField(name, false, Some(validator))))

}


