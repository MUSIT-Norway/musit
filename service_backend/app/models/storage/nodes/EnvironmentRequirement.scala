package models.storage.nodes

import models.storage.Interval
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
