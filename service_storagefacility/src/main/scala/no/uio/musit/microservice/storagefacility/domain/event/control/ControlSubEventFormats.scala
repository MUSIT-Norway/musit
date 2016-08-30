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

package no.uio.musit.microservice.storagefacility.domain.event.control

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.microservice.storagefacility.domain.event.{ EventType, MusitEventBase }
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ControlSubEventFormats {

  implicit object ControlSubEventsFormat extends Format[ControlSubEvent] {

    import SubTypeImplicits._

    // scalastyle:off cyclomatic.complexity method.length
    override def writes(cse: ControlSubEvent): JsValue = {
      cse match {
        case evt: ControlAlcohol => ctrlAlcoholFormat.writes(evt)
        case evt: ControlCleaning => ctrlCleaningFormat.writes(evt)
        case evt: ControlGas => ctrlGasFormat.writes(evt)
        case evt: ControlHypoxicAir => ctrlHypoxicAirFormat.writes(evt)
        case evt: ControlLightingCondition => ctrlLightingFormat.writes(evt)
        case evt: ControlMold => ctrlMoldFormat.writes(evt)
        case evt: ControlPest => ctrlPestFormat.writes(evt)
        case evt: ControlRelativeHumidity => ctrlHumidityFormat.writes(evt)
        case evt: ControlTemperature => ctrlTemperatureFormat.writes(evt)
      }
    } // scalastyle:on cyclomatic.complexity method.length

    // scalastyle:off cyclomatic.complexity method.length
    override def reads(json: JsValue): JsResult[ControlSubEvent] = {
      val tpeStr = (json \ "eventType").as[EventType]
      val entry = typedWithNameOption[CtrlSubEventType](tpeStr.name)

      entry.map {
        case CtrlAlcoholType => ctrlAlcoholFormat.reads(json)
        case CtrlCleaningType => ctrlCleaningFormat.reads(json)
        case CtrlGasType => ctrlGasFormat.reads(json)
        case CtrlHypoxicAirType => ctrlHypoxicAirFormat.reads(json)
        case CtrlLightingType => ctrlLightingFormat.reads(json)
        case CtrlMoldType => ctrlMoldFormat.reads(json)
        case CtrlPestType => ctrlPestFormat.reads(json)
        case CtrlHumidityType => ctrlHumidityFormat.reads(json)
        case CtrlTemperatureType => ctrlTemperatureFormat.reads(json)
      }.getOrElse(
        JsError(s"Unsupported sub-event $tpeStr for Control")
      )
    } // scalastyle:on cyclomatic.complexity method.length
  }

  /**
   * Provides implicits reads/writes/format for converting each specific
   * ControlSubEvent to/from JSON.
   */
  object SubTypeImplicits {

    import no.uio.musit.microservice.storagefacility.domain.event.observation.ObservationSubEventFormats.SubTypeImplicits._

    private[this] def baseFormats[A <: ControlSubEvent, B <: ObservationSubEvent](
      apply: (MusitEventBase, EventType, Boolean, Option[B]) => A,
      unapply: A => (MusitEventBase, EventType, Boolean, Option[B])
    )(implicit bformat: Format[B]): Format[A] = (
      __.format[MusitEventBase] and
      (__ \ "eventType").format[EventType] and
      (__ \ "ok").format[Boolean] and
      (__ \ "motivates").formatNullable[B]
    )(apply, unapply)

    implicit val ctrlAlcoholFormat: Format[ControlAlcohol] =
      baseFormats[ControlAlcohol, ObservationAlcohol](
        ControlAlcohol.apply,
        unlift(ControlAlcohol.unapply)
      )

    implicit val ctrlCleaningFormat: Format[ControlCleaning] =
      baseFormats[ControlCleaning, ObservationCleaning](
        ControlCleaning.apply,
        unlift(ControlCleaning.unapply)
      )

    implicit val ctrlGasFormat: Format[ControlGas] =
      baseFormats[ControlGas, ObservationGas](
        ControlGas.apply,
        unlift(ControlGas.unapply)
      )

    implicit val ctrlHypoxicAirFormat: Format[ControlHypoxicAir] =
      baseFormats[ControlHypoxicAir, ObservationHypoxicAir](
        ControlHypoxicAir.apply,
        unlift(ControlHypoxicAir.unapply)
      )

    implicit val ctrlLightingFormat: Format[ControlLightingCondition] =
      baseFormats[ControlLightingCondition, ObservationLightingCondition](
        ControlLightingCondition.apply,
        unlift(ControlLightingCondition.unapply)
      )

    implicit val ctrlMoldFormat: Format[ControlMold] =
      baseFormats[ControlMold, ObservationMold](
        ControlMold.apply,
        unlift(ControlMold.unapply)
      )

    implicit val ctrlPestFormat: Format[ControlPest] =
      baseFormats[ControlPest, ObservationPest](
        ControlPest.apply,
        unlift(ControlPest.unapply)
      )

    implicit val ctrlHumidityFormat: Format[ControlRelativeHumidity] =
      baseFormats[ControlRelativeHumidity, ObservationRelativeHumidity](
        ControlRelativeHumidity.apply,
        unlift(ControlRelativeHumidity.unapply)
      )

    implicit val ctrlTemperatureFormat: Format[ControlTemperature] =
      baseFormats[ControlTemperature, ObservationTemperature](
        ControlTemperature.apply,
        unlift(ControlTemperature.unapply)
      )

  }

}
