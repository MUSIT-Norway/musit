package models.conservation

import play.api.libs.json.{Format, Json}

case class TreatmentKeyword(
    id: Int,
    noTerm: String,
    enTerm: Option[String]
)

object TreatmentKeyword {
  implicit val format: Format[TreatmentKeyword] = Json.format[TreatmentKeyword]
}
