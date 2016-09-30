package no.uio.musit.microservice.storageAdmin.domain

import play.api.libs.json.{Format, Json}

case class SecurityAssessment(
  perimeterSecurity: Option[Boolean],
  theftProtection: Option[Boolean],
  fireProtection: Option[Boolean],
  waterDamageAssessment: Option[Boolean],
  routinesAndContingencyPlan: Option[Boolean]
)

object SecurityAssessment {
  implicit val format: Format[SecurityAssessment] = Json.format[SecurityAssessment]

  val empty = SecurityAssessment(None, None, None, None, None)
}

case class EnvironmentAssessment(
  relativeHumidity: Option[Boolean],
  temperatureAssessment: Option[Boolean],
  lightingCondition: Option[Boolean],
  preventiveConservation: Option[Boolean]
)

object EnvironmentAssessment {
  implicit val format: Format[EnvironmentAssessment] = Json.format[EnvironmentAssessment]

  val empty = EnvironmentAssessment(None, None, None, None)
}
