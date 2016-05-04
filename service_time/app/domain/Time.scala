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

package domain

import org.joda.time.DateTime.now
import org.joda.time.{DateTime, LocalDate, LocalTime}
import play.api.libs.json._

object DateFormats {
  val dateFormat: String = "yyyy-MM-dd"
  val timeFormat: String = "HH.mm.ss.SSS"
}

case class MusitError(message: String)

object MusitError {
  implicit val format: Format[MusitError] = Json.format[MusitError]
}

case class MusitTime(date: Option[LocalDate] = None, time: Option[LocalTime] = None)

object MusitTime {

  def fromDateTime(dateTime: DateTime): MusitTime =
    MusitTime(
      date = Some(dateTime.toLocalDate),
      time = Some(dateTime.toLocalTime)
    )

  implicit val localTimeWrites: Format[LocalTime] = Format[LocalTime](
    Reads.jodaLocalTimeReads(DateFormats.timeFormat),
    Writes.jodaLocalTimeWrites(DateFormats.timeFormat)
  )

  implicit val localDateWrites: Format[LocalDate] = Format[LocalDate](
    Reads.jodaLocalDateReads(DateFormats.dateFormat),
    Writes.jodaLocalDateWrites(DateFormats.dateFormat)
  )

  implicit val formats: Format[MusitTime] = Json.format[MusitTime]

  val dtRegex = "(date|time)".r

  def resolveFilter(filterString: String): Either[MusitError, MusitTime] = {
    dtRegex.findAllIn(filterString).toList.sorted match {
      case List("time") => Right(MusitTime(time = Some(now.toLocalTime)))
      case List("date") => Right(MusitTime(date = Some(now.toLocalDate)))
      case List("date", "time") => Right(fromDateTime(now))
      case _ => Left(MusitError("Only supports empty filter or filter on time, date or time and date"))
    }
  }

  def convertToNow(maybeFilter: Option[String]): Either[MusitError, MusitTime] =
    maybeFilter.map(f => resolveFilter(f)).getOrElse(Right(fromDateTime(now)))

}
