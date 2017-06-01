package models

import play.api.libs.json.{Format, Json}

case class FieldData(field: Option[String], value: String)

object FieldData {

  implicit val format: Format[FieldData] = Json.format[FieldData]

}
