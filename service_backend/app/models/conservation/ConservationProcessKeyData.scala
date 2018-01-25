package models.conservation

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

case class ConservationProcessKeyData(
    eventId: Long,
    caseNumber: Option[String],
    registeredDate: DateTime,
    registeredBy: ActorId,
    noKeyData: Option[Seq[String]],
    enKeyData: Option[Seq[String]]
)

object ConservationProcessKeyData extends WithDateTimeFormatters {
  implicit val format: Format[ConservationProcessKeyData] =
    Json.format[ConservationProcessKeyData]
}
