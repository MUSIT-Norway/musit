package models.analysis.events

import no.uio.musit.models.ActorId
import org.joda.time.DateTime

/**
 * Analysis' performed on sample objects may e.g. be part of a larger study. In
 * some of these cases it's desirable for the result(s) to have restrictions on
 * visibility for a set duration of time. Only when these restrictions are lifted,
 * typically when the study is published, will the results be publicly available.
 */
case class Restriction(
    by: ActorId,
    expirationDate: DateTime
)
