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

package no.uio.musit.microservice.time.domain
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalTime}
import play.api.libs.json._



abstract class MusitTime

case class Time(time:LocalTime) extends MusitTime
case class Date(date:LocalDate) extends MusitTime
case class DateTime(date:Date, time:Time) extends MusitTime

abstract class MusitJodaFilter

case class MusitDateFilter() extends MusitJodaFilter
case class MusitTimeFilter() extends MusitJodaFilter
case class MusitDateTimeFilter() extends MusitJodaFilter

object DateFormatDefinition {
  val dateFormat:String = "yyyy-MM-dd"
  val timeFormat:String = "HH.mm"
}

object Time {
  implicit val reads = Reads[Time] ( js =>
    js.validate[String].map[Time]( timeString =>
      Time(DateTimeFormat.forPattern(DateFormatDefinition.timeFormat).parseLocalTime(timeString))
    )
  )

  implicit val writes = new Writes[Time] {
    override def writes(time: Time):JsValue = JsString(time.formatted(DateFormatDefinition.timeFormat))
  }
}

object Date {
  implicit val reads = Reads[Date] ( js =>
    js.validate[String].map[Date]( dateString =>
      Date(DateTimeFormat.forPattern(DateFormatDefinition.dateFormat).parseLocalDate(dateString))
    )
  )

  implicit val writes = new Writes[Date] {
    override def writes(date: Date):JsValue = JsString(date.formatted(DateFormatDefinition.dateFormat))
  }
}

object DateTime {
  implicit val format = Json.format[DateTime]
}




