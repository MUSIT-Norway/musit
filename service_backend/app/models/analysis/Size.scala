package models.analysis

import play.api.libs.json.Json

case class Size(
    unit: String,
    value: Double
)

object Size {

  implicit val format = Json.format[Size]

}
