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

import no.uio.musit.microservice.storagefacility.domain.event.MusitSubEvent
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import play.api.libs.json.{Format, Json}

// TODO: Document me!!!
sealed trait ControlSubEvent[A <: ObservationSubEvent] extends MusitSubEvent {
  val ok: Boolean
  val observation: Option[A]
}

case class ControlAlcohol(
  ok: Boolean,
  observation: Option[ObservationAlcohol]
) extends ControlSubEvent[ObservationAlcohol]

object ControlAlcohol {
  implicit val formats: Format[ControlAlcohol] =
    Json.format[ControlAlcohol]
}

case class ControlCleaning(
  ok: Boolean,
  observation: Option[ObservationCleaning]
) extends ControlSubEvent[ObservationCleaning]

object ControlCleaning {
  implicit val formats: Format[ControlAlcohol] =
    Json.format[ControlAlcohol]
}

case class ControlGas(
  ok: Boolean,
  observation: Option[ObservationGas]
) extends ControlSubEvent[ObservationGas]

object ControlGas {
  implicit val formats: Format[ControlGas] =
    Json.format[ControlGas]
}

case class ControlHypoxicAir(
  ok: Boolean,
  observation: Option[ObservationHypoxicAir]
) extends ControlSubEvent[ObservationHypoxicAir]

object ControlHypoxicAir {
  implicit val formats: Format[ControlHypoxicAir] =
    Json.format[ControlHypoxicAir]
}

case class ControlLightingCondition(
  ok: Boolean,
  observation: Option[ObservationLightingCondition]
) extends ControlSubEvent[ObservationLightingCondition]

object ControlLightingCondition {
  implicit val formats: Format[ControlLightingCondition] =
    Json.format[ControlLightingCondition]
}

case class ControlMold(
  ok: Boolean,
  observation: Option[ObservationMold]
) extends ControlSubEvent[ObservationMold]

object ControlMold {
  implicit val formats: Format[ControlMold] =
    Json.format[ControlMold]
}

case class ControlPest(
  ok: Boolean,
  observation: Option[ObservationPest]
) extends ControlSubEvent[ObservationPest]

object ControlPest {
  implicit val formats: Format[ControlPest] =
    Json.format[ControlPest]
}

case class ControlRelativeHumidity(
  ok: Boolean,
  observation: Option[ObservationRelativeHumidity]
) extends ControlSubEvent[ObservationRelativeHumidity]

object ControlRelativeHumidity {
  implicit val formats: Format[ControlRelativeHumidity] =
    Json.format[ControlRelativeHumidity]
}

case class ControlTemperature(
  ok: Boolean,
  observation: Option[ObservationTemperature]
) extends ControlSubEvent[ObservationTemperature]

object ControlTemperature {
  implicit val formats: Format[ControlTemperature] =
    Json.format[ControlTemperature]
}
