package no.uio.musit.models

import no.uio.musit.models.ExternalRef.Divider
import play.api.libs.json.{Json, Reads, Writes}

case class ExternalRef(underlying: Seq[String]) {

  def toDbString = underlying.mkString(Divider, Divider, Divider)

}

object ExternalRef {
  val DividerChar = '|'
  val Divider     = DividerChar.toString

  def apply(opt: Option[String]): ExternalRef =
    ExternalRef(opt.toSeq)

  def apply(s: String): ExternalRef =
    ExternalRef(s.stripPrefix(Divider).stripPrefix(Divider).split(DividerChar))

  implicit val reads: Reads[ExternalRef] = Reads { jsv =>
    jsv.validate[Seq[String]].map(ExternalRef.apply)
  }

  implicit val writes: Writes[ExternalRef] = Writes { ef =>
    Json.toJson(ef.underlying)
  }

}
