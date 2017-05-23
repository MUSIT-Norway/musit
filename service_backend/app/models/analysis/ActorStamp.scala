package models.analysis

import no.uio.musit.models.ActorId
import org.joda.time.DateTime
import play.api.libs.json.Json

case class ActorStamp(
    user: ActorId,
    date: DateTime
)

object ActorStamp {

  implicit val format = Json.format[ActorStamp]

}
