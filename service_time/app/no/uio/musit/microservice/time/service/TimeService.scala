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

package no.uio.musit.microservice.time.service

import no.uio.musit.microservice.time.domain._

trait TimeService {

  def now = org.joda.time.DateTime.now

  def getSortedFilters(filterString: String): List[String] = filterString match {
    case filter if filter != null => "(date|time)".r.findAllIn(filter).toList.sorted
    case _ => List()
  }

  def getMusitFilter(filterString: String): Option[MusitJodaFilter] = filterString match {
    case filter if filter != null && filter.nonEmpty =>
      getSortedFilters(filter) match {
        case List("date", "time") => Some(MusitDateTimeFilter)
        case List("time") => Some(MusitTimeFilter)
        case List("date") => Some(MusitDateFilter)
        case _ => throw new IllegalArgumentException("Only supports empty filter or filter on time, date or time and date")
      }
    case _ => None
  }

  def getNow(filter: Option[MusitJodaFilter]): MusitTime = {
    filter.map {
      case MusitDateTimeFilter => DateTime(Date(now.toLocalDate), Time(now.toLocalTime))
      case MusitDateFilter => Date(now.toLocalDate)
      case MusitTimeFilter => Time(now.toLocalTime)
    }.getOrElse(DateTime(Date(now.toLocalDate), Time(now.toLocalTime)))
  }
}
