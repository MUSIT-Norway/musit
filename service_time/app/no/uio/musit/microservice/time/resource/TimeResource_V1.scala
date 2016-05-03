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
package no.uio.musit.microservice.time.resource

import no.uio.musit.microservice.time.domain._
import no.uio.musit.microservice.time.service.TimeService
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

class TimeResource_V1 extends Controller with TimeService {



  def actionGetNow = Action.async { request =>
    var filter:Option[MusitJodaFilter] = None

    val filterString:String = request.getQueryString("filter").orNull

    if (filterString != null && filterString.size > 0) {
      val list = "(date|time)".r.findAllIn(filterString).toList.sorted


        list match {
          case List ("date","time") => filter= Some( new MusitDateTimeFilter)
          case List ("time")        => filter= Some( new MusitTimeFilter)
          case List ("date")        => filter= Some( new MusitDateFilter)
          case                              _ => throw new IllegalArgumentException("Only supports empty filter or filter on time, date or time and date")
        }
    }
    val now = getNow(filter)
    Future.successful(Ok(Json.toJson(now)))
  }

}
