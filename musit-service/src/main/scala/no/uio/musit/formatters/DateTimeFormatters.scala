package no.uio.musit.formatters

import org.joda.time.DateTime
import play.api.libs.json._

/**
 * Converters helping to converting DateTime to/from UTC/ISO formatted dates.
 */
trait WithDateTimeFormatters {

  val defaultDateTimePattern: String    = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    JodaReads
      .jodaDateReads(defaultDateTimePattern)
      .orElse(JodaReads.jodaDateReads(readDateTimeMillisPattern)),
    JodaWrites.jodaDateWrites(defaultDateTimePattern)
  )
}

object DateTimeFormatters extends WithDateTimeFormatters
