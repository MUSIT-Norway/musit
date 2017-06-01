package no.uio.musit.security

import java.util.UUID

import no.uio.musit.models.MusitUUID
import play.api.libs.json.{JsString, Writes, _}

import scala.util.Try

case class ClientId(underlying: UUID) extends MusitUUID

object ClientId {

  implicit val reads: Reads[ClientId] =
    __.read[String].map(s => ClientId(UUID.fromString(s)))

  implicit val writes: Writes[ClientId] = Writes(id => JsString(id.asString))

  implicit def fromUUID(uuid: UUID): ClientId = ClientId(uuid)

  def validate(str: String): Try[UUID] = Try(UUID.fromString(str))

  def generate(): ClientId = ClientId(UUID.randomUUID())

}
