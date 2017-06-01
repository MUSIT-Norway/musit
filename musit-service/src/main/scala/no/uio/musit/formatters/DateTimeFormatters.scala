package no.uio.musit.formatters

import org.joda.time.DateTime
import play.api.libs.json.{Format, Reads, Writes}

/**
 * Converters helping to converting DateTime to/from UTC/ISO formatted dates.
 */
trait WithDateTimeFormatters {

  val defaultDateTimePattern: String    = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    Reads
      .jodaDateReads(defaultDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultDateTimePattern)
  )
}

object DateTimeFormatters extends WithDateTimeFormatters
