package models.analysis

import play.api.libs.json.Json

case class SampleType(
    value: String,
    subTypeValue: Option[String]
)

object SampleType {

  implicit val format = Json.format[SampleType]

}
