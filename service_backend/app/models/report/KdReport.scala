package models.report

import play.api.libs.json.{Format, Json}

case class KdReport(
    totalArea: Double,
    perimeterSecurity: Double,
    theftProtection: Double,
    fireProtection: Double,
    waterDamageAssessment: Double,
    routinesAndContingencyPlan: Double
)

object KdReport {

  implicit val format: Format[KdReport] =
    Json.format[KdReport]
}
