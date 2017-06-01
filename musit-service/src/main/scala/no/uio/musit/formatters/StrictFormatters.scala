package no.uio.musit.formatters

import play.api.libs.json.Format
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

object StrictFormatters {

  /**
   * Strict String JSON parser. It will allow writing Strings that are longer
   * than the max value. But it will fail trying to parse a JSON String that is
   * longer than the given max length.
   */
  def maxCharsFormat(max: Int) = Format(maxLength[String](max), StringWrites)

}
