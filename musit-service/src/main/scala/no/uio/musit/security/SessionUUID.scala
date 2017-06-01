package no.uio.musit.security

import java.util.UUID

import no.uio.musit.models.{MusitUUID, MusitUUIDOps}
import play.api.libs.json.{JsString, Reads, Writes, __}

case class SessionUUID(underlying: UUID) extends MusitUUID {

  def asBearerToken: BearerToken = BearerToken.fromMusitUUID(this)

}

object SessionUUID extends MusitUUIDOps[SessionUUID] {

  implicit val reads: Reads[SessionUUID] =
    __.read[String].map(s => SessionUUID(UUID.fromString(s)))

  implicit val writes: Writes[SessionUUID] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): SessionUUID = SessionUUID(uuid)

  override def generate() = SessionUUID(UUID.randomUUID())

  def fromBearerToken(token: BearerToken): SessionUUID = {
    unsafeFromString(token.underlying)
  }

}
