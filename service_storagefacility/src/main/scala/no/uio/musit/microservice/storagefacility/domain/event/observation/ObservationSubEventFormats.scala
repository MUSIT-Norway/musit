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

package no.uio.musit.microservice.storagefacility.domain.event.observation

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.ObservationSubEvents._
import no.uio.musit.microservice.storagefacility.domain.event.{EventType, BaseEvent}
import no.uio.musit.microservice.storagefacility.domain.{FromToDouble, LifeCycle}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ObservationSubEventFormats {

  implicit object ObservationSubEventFormat extends Format[ObservationSubEvent] {

    import SubTypeImplicits._

    // scalastyle:off cyclomatic.complexity method.length
    override def writes(ose: ObservationSubEvent): JsValue = {
      ose match {
        case evt: ObservationAlcohol => obsAlcoholFormat.writes(evt)
        case evt: ObservationCleaning => obsCleaningFormat.writes(evt)
        case evt: ObservationGas => obsGasFormat.writes(evt)
        case evt: ObservationHypoxicAir => obsHypoxicAirFormat.writes(evt)
        case evt: ObservationLightingCondition => obsLightingFormat.writes(evt)
        case evt: ObservationMold => obsMoldFormat.writes(evt)
        case evt: ObservationPerimeterSecurity => obsPerimeterFormat.writes(evt)
        case evt: ObservationPest => obsPestFormat.writes(evt)
        case evt: ObservationRelativeHumidity => obsRelHumidityFormat.writes(evt)
        case evt: ObservationFireProtection => obsFireFormat.writes(evt)
        case evt: ObservationTheftProtection => obsTheftFormat.writes(evt)
        case evt: ObservationWaterDamageAssessment => obsWaterFormat.writes(evt)
        case evt: ObservationTemperature => obsTemperatureFormat.writes(evt)
      }
    } // scalastyle:on cyclomatic.complexity method.length

    // scalastyle:off cyclomatic.complexity method.length
    override def reads(json: JsValue): JsResult[ObservationSubEvent] = {
      val tpeStr = (json \ "eventType").as[EventType]
      val entry = withNameInsensitiveOption(tpeStr.name)

      entry.map {
        case ObsAlcoholType => obsAlcoholFormat.reads(json)
        case ObsCleaningType => obsCleaningFormat.reads(json)
        case ObsGasType => obsGasFormat.reads(json)
        case ObsHypoxicAirType => obsHypoxicAirFormat.reads(json)
        case ObsLightingType => obsLightingFormat.reads(json)
        case ObsMoldType => obsMoldFormat.reads(json)
        case ObsPerimeterType => obsPerimeterFormat.reads(json)
        case ObsPestType => obsPestFormat.reads(json)
        case ObsHumidityType => obsRelHumidityFormat.reads(json)
        case ObsFireType => obsFireFormat.reads(json)
        case ObsTheftType => obsTheftFormat.reads(json)
        case ObsWaterDamageType => obsWaterFormat.reads(json)
        case ObsTemperatureType => obsTemperatureFormat.reads(json)
      }.getOrElse(
        JsError(s"Unsupported sub-event $tpeStr for Observation")
      )
    } // scalastyle:on cyclomatic.complexity method.length
  }

  /**
   * Provides implicits reads/writes/format for converting each specific
   * ObservationSubEvent to/from JSON.
   */
  object SubTypeImplicits {

    implicit val obsTemperatureFormat: Format[ObservationTemperature] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      __.format[FromToDouble]
    )(ObservationTemperature.apply, unlift(ObservationTemperature.unapply))

    implicit val obsHypoxicAirFormat: Format[ObservationHypoxicAir] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      __.format[FromToDouble]
    )(ObservationHypoxicAir.apply, unlift(ObservationHypoxicAir.unapply))

    implicit val obsRelHumidityFormat: Format[ObservationRelativeHumidity] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      __.format[FromToDouble]
    )(ObservationRelativeHumidity.apply, unlift(ObservationRelativeHumidity.unapply))

    implicit val obsLightingFormat: Format[ObservationLightingCondition] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "lightingCondition").formatNullable[String]
    )(ObservationLightingCondition.apply, unlift(ObservationLightingCondition.unapply))

    implicit val obsCleaningFormat: Format[ObservationCleaning] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "cleaning").formatNullable[String]
    )(ObservationCleaning.apply, unlift(ObservationCleaning.unapply))

    implicit val obsGasFormat: Format[ObservationGas] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "gas").formatNullable[String]
    )(ObservationGas.apply, unlift(ObservationGas.unapply))

    implicit val obsMoldFormat: Format[ObservationMold] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "mold").formatNullable[String]
    )(ObservationMold.apply, unlift(ObservationMold.unapply))

    implicit val obsTheftFormat: Format[ObservationTheftProtection] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "theftProtection").formatNullable[String]
    )(ObservationTheftProtection.apply, unlift(ObservationTheftProtection.unapply))

    implicit val obsFireFormat: Format[ObservationFireProtection] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "fireProtection").formatNullable[String]
    )(ObservationFireProtection.apply, unlift(ObservationFireProtection.unapply))

    implicit val obsPerimeterFormat: Format[ObservationPerimeterSecurity] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "perimeterSecurity").formatNullable[String]
    )(ObservationPerimeterSecurity.apply, unlift(ObservationPerimeterSecurity.unapply))

    implicit val obsWaterFormat: Format[ObservationWaterDamageAssessment] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "waterDamageAssessment").formatNullable[String]
    )(ObservationWaterDamageAssessment.apply, unlift(ObservationWaterDamageAssessment.unapply))

    implicit val obsPestFormat: Format[ObservationPest] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "identification").formatNullable[String] and
      (__ \ "lifecycles").format[Seq[LifeCycle]]
    )(ObservationPest.apply, unlift(ObservationPest.unapply))

    implicit val obsAlcoholFormat: Format[ObservationAlcohol] = (
      __.format[BaseEvent] and
      (__ \ "eventType").format[EventType] and
      (__ \ "condition").formatNullable[String] and
      (__ \ "volume").formatNullable[Double]
    )(ObservationAlcohol.apply, unlift(ObservationAlcohol.unapply))

  }

}
