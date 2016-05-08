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
package no.uio.musit.microservices.time.domain

import no.uio.musit.microservices.common.domain.MusitFilter
import org.joda.time.{LocalDate, LocalTime}
import play.api.libs.json._

object DateFormats {
  val dateFormat: String = "yyyy-MM-dd"
  val timeFormat: String = "HH.mm.ss.SSS"
}

case class MusitTime(date: Option[LocalDate] = None, time: Option[LocalTime] = None)

object MusitTime {

  implicit val localTimeWrites: Format[LocalTime] = Format[LocalTime](
    Reads.jodaLocalTimeReads(DateFormats.timeFormat),
    Writes.jodaLocalTimeWrites(DateFormats.timeFormat)
  )

  implicit val localDateWrites: Format[LocalDate] = Format[LocalDate](
    Reads.jodaLocalDateReads(DateFormats.dateFormat),
    Writes.jodaLocalDateWrites(DateFormats.dateFormat)
  )

  implicit val formats: Format[MusitTime] = Json.format[MusitTime]
}

object MusitDateTimeFilter extends MusitFilter(List("date","time"))
object MusitDateFilter extends MusitFilter(List("date"))
object MusitTimeFilter extends MusitFilter(List("time"))