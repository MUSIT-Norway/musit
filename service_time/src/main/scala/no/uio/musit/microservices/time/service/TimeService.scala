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
package no.uio.musit.microservices.time.service

import no.uio.musit.microservices.common.domain.{ MusitError, MusitFilter }
import no.uio.musit.microservices.time.domain._
import org.joda.time.DateTime
import org.joda.time.DateTime.now

class TimeService {

  def convertToNow(maybeFilter: Option[MusitFilter]): Either[MusitError, MusitTime] =
    maybeFilter.map(f => resolveNow(f)).getOrElse(Right(fromDateTime(now)))

  def resolveNow(filter: MusitFilter): Either[MusitError, MusitTime] = filter match {
    case MusitFilter(List("time")) => Right(MusitTime(time = Some(now.toLocalTime)))
    case MusitFilter(List("date")) => Right(MusitTime(date = Some(now.toLocalDate)))
    case MusitFilter(List("date", "time")) => Right(fromDateTime(now))
    case _ => Left(MusitError(message = "Only supports empty filter or filter on time, date or time and date"))
  }

  def fromDateTime(dateTime: DateTime): MusitTime =
    MusitTime(
      date = Some(dateTime.toLocalDate),
      time = Some(dateTime.toLocalTime)
    )

}