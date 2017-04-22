package models.analysis

import play.api.libs.json.Json

case class ExternalId(value: String, source: Option[String])

object ExternalId {

  implicit val format = Json.format[ExternalId]

}
