package no.uio.musit.models

import no.uio.musit.models.CaseNumbers.Divider
import play.api.libs.json.{Json, Reads, Writes}

case class CaseNumbers(underlying: Seq[String]) {

  def toDbString = underlying.mkString(Divider, Divider, Divider)

}

object CaseNumbers {
  val DividerChar = '|'
  val Divider     = DividerChar.toString

  def apply(opt: Option[String]): CaseNumbers =
    CaseNumbers(opt.toSeq)

  def apply(s: String): CaseNumbers =
    CaseNumbers(s.stripPrefix(Divider).stripPrefix(Divider).split(DividerChar))

  implicit val reads: Reads[CaseNumbers] = Reads { jsv =>
    jsv.validate[Seq[String]].map(CaseNumbers.apply)
  }

  implicit val writes: Writes[CaseNumbers] = Writes { ef =>
    Json.toJson(ef.underlying)
  }

}
