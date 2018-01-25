package models.conservation

import play.api.libs.json.{Format, Json}

case class ConservationProcessKeyData(
    eventId: Long,
    caseNumber: Option[String],
    noKeyData: Option[Seq[String]],
    enKeyData: Option[Seq[String]]
)

object ConservationProcessKeyData {
  implicit val format: Format[ConservationProcessKeyData] =
    Json.format[ConservationProcessKeyData]
}
