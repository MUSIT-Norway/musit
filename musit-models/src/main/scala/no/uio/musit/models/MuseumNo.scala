package no.uio.musit.models

import play.api.libs.json._

trait MuseumNumber {
  val value: String

  /**
   * A sequence of non-digits, followed by a sequence of digits, followed by an
   * optional tail starting with a non-digit (and then whatever).
   *
   * NOTE: This regex will _not_ match the full number sequence for MuseumNumbers
   * that have the form "E10-15" (note the hyphen). Such MuseumNumbers indicate
   * a sequence of objects that are registered in the same entry. One important
   * point regarding these entries is that the objects are do not necessarily
   * share any properties. They might not even be related in any other way than
   * being registered at the same point in history.
   */
  val regExp = """\A\D*(\d+)(?:\D.*)?\z""".r

  /**
   * The number part of a museumNo
   */
  def asNumber: Option[Long] = {
    val optM = regExp.findFirstMatchIn(value)

    // This regular expression is designed to only return one group. Per def of
    // this re, this should always be possible (within reasonable length of
    // museumNo!) and never throw any exceptions.
    optM.map { m =>
      assert(m.groupCount == 1) // TODO: Don't throw Exception here!
      m.group(1).toLong
    }
  }
}

case class MuseumNo(value: String) extends MuseumNumber

object MuseumNo {
  implicit val reads: Reads[MuseumNo]   = __.read[String].map(MuseumNo.apply)
  implicit val writes: Writes[MuseumNo] = Writes(mn => JsString(mn.value))
}

case class SubNo(value: String) extends MuseumNumber

object SubNo {
  implicit val reads: Reads[SubNo]   = __.read[String].map(SubNo.apply)
  implicit val writes: Writes[SubNo] = Writes(mn => JsString(mn.value))
}
