/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.formatters

import org.joda.time.DateTime
import play.api.libs.json.{ Format, Reads, Writes }

/**
 * Converters helping to converting DateTime to/from UTC/ISO formatted dates.
 */
trait WithDateTimeFormatters {

  val defaultReadDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZZ"
  val readDateTimeMillisPattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"

  // Joda date formatter
  implicit val dateTimeFormatter = Format[DateTime](
    Reads.jodaDateReads(defaultReadDateTimePattern)
      .orElse(Reads.jodaDateReads(readDateTimeMillisPattern)),
    Writes.jodaDateWrites(defaultReadDateTimePattern)
  )

  implicit def asDateTime(jud: java.util.Date): DateTime = new DateTime(jud)

  implicit def asOptDateTime(maybeJud: Option[java.util.Date]): Option[DateTime] =
    maybeJud.map(jud => asDateTime(jud))

}

object DateTimeFormatters extends WithDateTimeFormatters