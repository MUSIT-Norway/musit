package models.storage.nodes

import play.api.libs.json.{Format, Json}

// FIXME Fields are required according to requirements
case class SecurityAssessment(
    perimeter: Option[Boolean],
    theftProtection: Option[Boolean],
    fireProtection: Option[Boolean],
    waterDamage: Option[Boolean],
    routinesAndContingencyPlan: Option[Boolean]
)

object SecurityAssessment {

  lazy val empty = SecurityAssessment(None, None, None, None, None)

  implicit val format: Format[SecurityAssessment] =
    Json.format[SecurityAssessment]
}
