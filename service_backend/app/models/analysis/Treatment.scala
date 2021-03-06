package models.analysis

import play.api.libs.json.{Format, Json}

case class Treatment(
    treatmentId: Int,
    noTreatment: String,
    enTreatment: String
)

object Treatment {
  implicit val format: Format[Treatment] = Json.format[Treatment]
}
