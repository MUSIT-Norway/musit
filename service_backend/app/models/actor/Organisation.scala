package models.actor

import no.uio.musit.models.OrgId
import play.api.libs.json.{Json, Reads, Writes}

case class WordList(underlying: Seq[String]) {

  def asDbString = underlying.mkString("|", "|", "|")

}

object WordList {

  def fromDbString(str: String): WordList = {
    val words = str.stripPrefix("|").stripSuffix("|").split("\\|")
    WordList(words)
  }

  def fromOptDbString(mstr: Option[String]): Option[WordList] =
    mstr.map(WordList.fromDbString)

  implicit def wordListConverter(strSeq: Seq[String]): WordList = {
    WordList(strSeq)
  }

  implicit def wordListAsSeq(wl: WordList): Seq[String] = {
    wl.underlying
  }

  implicit val reads: Reads[WordList] = Reads { jsv =>
    jsv.validate[Seq[String]].map(WordList.apply)
  }

  implicit val writes: Writes[WordList] = Writes { wl =>
    Json.toJson(wl.underlying)
  }

}

/**
 * Domain Organization
 */
case class Organisation(
    id: Option[OrgId],
    fullName: String,
    tel: Option[String],
    web: Option[String],
    synonyms: Option[WordList],
    serviceTags: Option[WordList],
    contact: Option[String],
    email: Option[String]
)

object Organisation {

  implicit val format = Json.format[Organisation]
}
