package models.conservation

import play.api.libs.json.{Format, Json}

case class TreatmentMaterial(
    id: Int,
    noTerm: String,
    enTerm: Option[String]
)

object TreatmentMaterial {
  implicit val format: Format[TreatmentMaterial] = Json.format[TreatmentMaterial]
}
