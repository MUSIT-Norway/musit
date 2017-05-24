package models.storage.nodes

import play.api.libs.json.{Format, Json}

// FIXME Fields are required according to requirements
case class EnvironmentAssessment(
    relativeHumidity: Option[Boolean],
    temperature: Option[Boolean],
    lightingCondition: Option[Boolean],
    preventiveConservation: Option[Boolean]
)

object EnvironmentAssessment {

  lazy val empty = EnvironmentAssessment(None, None, None, None)

  implicit val format: Format[EnvironmentAssessment] =
    Json.format[EnvironmentAssessment]
}
