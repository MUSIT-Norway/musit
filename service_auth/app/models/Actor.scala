package models

import play.api.libs.json.{Format, Json}

case class Actor(
  id: Int,
  fn: String,
  dataportenId: Option[String],
  applicationId: Option[String]
)

object Actor {
  implicit def format: Format[Actor] = Json.format[Actor]
}
