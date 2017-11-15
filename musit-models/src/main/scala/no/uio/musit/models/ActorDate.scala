package no.uio.musit.models

import org.joda.time.DateTime

case class ActorDate(
    user: ActorId,
    date: DateTime
)
