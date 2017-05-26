package models.analysis.events

import models.analysis.ActorStamp
import no.uio.musit.models.{ActorId, CaseNumbers}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

/**
 * Analysis' performed on sample objects may e.g. be part of a larger study. In
 * some of these cases it's desirable for the result(s) to have restrictions on
 * visibility for a set duration of time. Only when these restrictions are lifted,
 * typically when the study is published, will the results be publicly available.
 */
case class Restriction(
    requester: ActorId,
    expirationDate: DateTime,
    reason: String,
    caseNumbers: Option[CaseNumbers] = None,
    registeredStamp: Option[ActorStamp] = None,
    cancelledStamp: Option[ActorStamp] = None,
    cancelledReason: Option[String] = None
)

object Restriction {

  implicit val f: Format[Restriction] = Json.format[Restriction]

}
