package models.conservation.events

import no.uio.musit.models.{ActorId, EventId}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import no.uio.musit.formatters.WithDateTimeFormatters

case class EventActorRoleDate(
    eventId: EventId,
    roleId: Int,
    actorId: ActorId,
    actorDate: Option[DateTime]
)

object EventActorRoleDate extends WithDateTimeFormatters {
  val tupled          = (EventActorRoleDate.apply _).tupled
  implicit val format = Json.format[EventActorRoleDate]
}

case class EventRole(roleId: Int, noRole: String, enRole: String, roleFor: String)

object EventRole extends WithDateTimeFormatters {
  implicit val format: Format[EventRole] = Json.format[EventRole]
}
