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

import no.uio.musit.microservice.storagefacility.domain.event.MusitSubEvent
import no.uio.musit.microservice.storagefacility.domain.{FromToDouble, LifeCycle}
import play.api.libs.json.{Format, Json}

object ObservationSubEvents {

  // TODO: Document me!!!
  sealed trait ObservationSubEvent extends MusitSubEvent {
    val note: Option[String]
  }

  // TODO: Document me!!!
  sealed trait ObservationFromTo extends ObservationSubEvent {
    val range: FromToDouble
  }

  case class ObservationRelativeHumidity(
    note: Option[String],
    range: FromToDouble
  ) extends ObservationFromTo

  object ObservationRelativeHumidity {
    implicit val formats: Format[ObservationRelativeHumidity] =
      Json.format[ObservationRelativeHumidity]
  }

  case class ObservationTemperature(
    note: Option[String],
    range: FromToDouble
  ) extends ObservationFromTo

  object ObservationTemperature {
    implicit val formats: Format[ObservationTemperature] =
      Json.format[ObservationTemperature]
  }

  case class ObservationHypoxicAir(
    note: Option[String],
    range: FromToDouble
  ) extends ObservationFromTo

  object ObservationHypoxicAir {
    implicit val formats: Format[ObservationHypoxicAir] =
      Json.format[ObservationHypoxicAir]
  }

  case class ObservationLightingCondition(
    note: Option[String],
    lightingCondition: Option[String]
  ) extends ObservationSubEvent

  object ObservationLightingCondition {
    implicit val formats: Format[ObservationLightingCondition] =
      Json.format[ObservationLightingCondition]
  }

  case class ObservationCleaning(
    note: Option[String],
    cleaning: Option[String]
  ) extends ObservationSubEvent

  object ObservationCleaning {
    implicit val formats: Format[ObservationCleaning] =
      Json.format[ObservationCleaning]
  }

  case class ObservationGas(
    note: Option[String],
    gas: Option[String]
  ) extends ObservationSubEvent

  object ObservationGas {
    implicit val formats: Format[ObservationGas] =
      Json.format[ObservationGas]
  }

  case class ObservationMold(
    note: Option[String],
    mold: Option[String]
  ) extends ObservationSubEvent

  object ObservationMold {
    implicit val formats: Format[ObservationMold] =
      Json.format[ObservationMold]
  }

  case class ObservationTheftProtection(
    note: Option[String],
    theftProtection: Option[String]
  ) extends ObservationSubEvent

  object ObservationTheftProtection {
    implicit val formats: Format[ObservationTheftProtection] =
      Json.format[ObservationTheftProtection]
  }

  case class ObservationFireProtection(
    note: Option[String],
    fireProtection: Option[String]
  ) extends ObservationSubEvent

  object ObservationFireProtection {
    implicit val formats: Format[ObservationFireProtection] =
      Json.format[ObservationFireProtection]
  }

  case class ObservationPerimeterSecurity(
    note: Option[String],
    perimeterSecurity: Option[String]
  ) extends ObservationSubEvent

  object ObservationPerimeterSecurity {
    implicit val formats: Format[ObservationPerimeterSecurity] =
      Json.format[ObservationPerimeterSecurity]
  }

  case class ObservationWaterDamageAssessment(
    note: Option[String],
    waterDamageAssessment: Option[String]
  ) extends ObservationSubEvent

  object ObservationWaterDamageAssessment {
    implicit val formats: Format[ObservationWaterDamageAssessment] =
      Json.format[ObservationWaterDamageAssessment]
  }

  case class ObservationPest(
    note: Option[String],
    identification: Option[String],
    lifecycles: Seq[LifeCycle]
  ) extends ObservationSubEvent

  object ObservationPest {
    implicit val formats: Format[ObservationPest] =
      Json.format[ObservationPest]
  }

  case class ObservationAlcohol(
    note: Option[String],
    condition: Option[String],
    volume: Option[Double]
  ) extends ObservationSubEvent

  object ObservationAlcohol {
    implicit val formats: Format[ObservationAlcohol] =
      Json.format[ObservationAlcohol]
  }

}