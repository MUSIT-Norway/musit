package models.storage

import play.api.libs.json.Json

case class LifeCycle(stage: Option[String], quantity: Option[Int])

object LifeCycle {
  implicit val format = Json.format[LifeCycle]
}
