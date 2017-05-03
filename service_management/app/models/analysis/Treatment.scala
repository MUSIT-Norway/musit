package models.analysis

import play.api.libs.json.{Format, Json}

case class Treatment(
    no_treatment: String,
    en_treatment: String
)

object Treatment {
  implicit val format: Format[Treatment] = Json.format[Treatment]
}
