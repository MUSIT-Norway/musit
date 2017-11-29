package models.conservation

import play.api.libs.json.{Format, Json}

case class ConditionCode(
    conditionCode: Int,
    noCondition: String,
    enCondition: String
)

object ConditionCode {
  implicit val format: Format[ConditionCode] = Json.format[ConditionCode]
}
