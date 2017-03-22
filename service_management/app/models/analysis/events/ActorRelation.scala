package models.analysis.events

import no.uio.musit.models.ActorId
import play.api.libs.json.{Format, Json}

case class ActorRelation(
    actorId: ActorId,
    role: String
)

object ActorRelation {

  implicit val format: Format[ActorRelation] = Json.format[ActorRelation]

}
