package models.conservation.events

import no.uio.musit.models.{ActorId}
import no.uio.musit.formatters.WithDateTimeFormatters
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

case class ActorRoleDate(roleId: Int, actorId: ActorId, date: Option[DateTime])

object ActorRoleDate extends WithDateTimeFormatters {
  implicit val format: Format[ActorRoleDate] = Json.format[ActorRoleDate]
}
