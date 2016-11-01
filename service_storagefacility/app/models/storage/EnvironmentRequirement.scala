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

package models.storage

import models.Interval
import no.uio.musit.formatters.StrictFormatters._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class EnvironmentRequirement(
  temperature: Option[Interval[Double]],
  relativeHumidity: Option[Interval[Double]],
  hypoxicAir: Option[Interval[Double]],
  cleaning: Option[String],
  lightingCondition: Option[String],
  comment: Option[String]
)

object EnvironmentRequirement {

  implicit val format: Format[EnvironmentRequirement] = (
    (__ \ "temperature").formatNullable[Interval[Double]] and
    (__ \ "relativeHumidity").formatNullable[Interval[Double]] and
    (__ \ "hypoxicAir").formatNullable[Interval[Double]] and
    (__ \ "cleaning").formatNullable[String](maxCharsFormat(100)) and
    (__ \ "lightingCondition").formatNullable[String](maxCharsFormat(100)) and
    (__ \ "comment").formatNullable[String](maxCharsFormat(250))
  )(EnvironmentRequirement.apply, unlift(EnvironmentRequirement.unapply))

}