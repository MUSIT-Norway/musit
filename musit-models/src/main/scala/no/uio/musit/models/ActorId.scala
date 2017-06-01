package no.uio.musit.models

import java.util.UUID

import play.api.libs.json._

case class ActorId(underlying: UUID) extends MusitUUID

object ActorId extends MusitUUIDOps[ActorId] {

  implicit val reads: Reads[ActorId] =
    __.read[String].map(s => ActorId(UUID.fromString(s)))

  implicit val writes: Writes[ActorId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): ActorId = ActorId(uuid)

  def generate(): ActorId = ActorId(UUID.randomUUID())

}
