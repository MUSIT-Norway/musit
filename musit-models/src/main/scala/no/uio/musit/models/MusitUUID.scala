package no.uio.musit.models

import java.util.{NoSuchElementException, UUID}

import scala.util.Try

trait MusitUUID {

  val underlying: UUID

  def asString: String = underlying.toString

}

trait MusitUUIDOps[T <: MusitUUID] {

  implicit def fromUUID(uuid: UUID): T

  lazy val empty = unsafeFromString("00000000-0000-0000-0000-000000000000")

  @throws(classOf[NoSuchElementException])
  def unsafeFromString(str: String): T = fromString(str).get

  def validate(str: String): Try[UUID] = Try(UUID.fromString(str))

  def generate(): T

  def generateAsOpt(): Option[T] = Option(generate())

  def fromString(str: String): Option[T] = validate(str).toOption.map(fromUUID)

}
