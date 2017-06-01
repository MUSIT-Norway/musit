package no.uio.musit.models

import play.api.data.validation._
import play.api.libs.json._
import play.api.libs.json.Reads._

case class Email(value: String) extends AnyVal {

  def startsWith(str: String): Boolean = {
    value.toLowerCase.startsWith(str.toLowerCase)
  }

}

object Email {

  def validate(str: String): Option[Email] = {
    Constraints.emailAddress(str) match {
      case Valid         => Option(Email.fromString(str))
      case Invalid(errs) => None
    }
  }

  def fromString(str: String): Email = Email(str.toLowerCase)

  implicit val reads: Reads[Email] = {
    __.read[String](email).map(s => Email(s.toLowerCase))
  }

  implicit val writes: Writes[Email] = Writes(oid => JsString(oid.value))

}
