package models

import play.api.libs.json.{Format, Json}

case class LabelData(uuid: String, data: Seq[FieldData])

object LabelData {

  implicit val format: Format[LabelData] = Json.format[LabelData]

}
