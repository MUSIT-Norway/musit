package models.dto

import java.sql.{Timestamp => JSqlTimestamp}

import no.uio.musit.models.{ActorId, EventId}
import play.api.libs.json.JsValue

case class AnalysisEventDto(
  id: Option[EventId],
  eventType: Int,
  eventDate: JSqlTimestamp,
  registeredBy: Option[ActorId],
  registeredDate: Option[JSqlTimestamp],
  json: JsValue
)
