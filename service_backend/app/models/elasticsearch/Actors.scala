package models.elasticsearch

import models.analysis.ActorStamp
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ActorId
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}

object Actors extends WithDateTimeFormatters {
  case class ActorSearchStamp(
      id: ActorId,
      date: DateTime,
      name: Option[String]
  )

  object ActorSearchStamp {
    implicit val writes: Writes[ActorSearchStamp] = Json.writes[ActorSearchStamp]

    def apply(
        idOpt: Option[ActorId],
        dateOpt: Option[DateTime],
        actorNames: ActorNames
    ): Option[ActorSearchStamp] =
      for {
        id   <- idOpt
        date <- dateOpt
      } yield ActorSearchStamp(id, date, actorNames.nameFor(id))

    def apply(as: ActorStamp, actorNames: ActorNames): ActorSearchStamp =
      ActorSearchStamp(as.user, as.date, actorNames.nameFor(as.user))
  }

  case class ActorSearch(
      id: ActorId,
      name: Option[String]
  )

  object ActorSearch {
    implicit val writes: Writes[ActorSearch] = Json.writes[ActorSearch]
  }

}
