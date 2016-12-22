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

package no.uio.musit.service

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.routing.Router.Tags
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class AccessLogFilter @Inject() (implicit val mat: Materializer) extends Filter {

  val logger = Logger("accesslog")

  type NextFilter = RequestHeader => Future[Result]

  def apply(nextFilter: NextFilter)(rh: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(rh).map { response =>
      val action = rh.tags(Tags.RouteController) +
        "." + rh.tags(Tags.RouteActionMethod)
      val endTime = System.currentTimeMillis
      val procTime = endTime - startTime

      logger.info(
        s"${rh.remoteAddress} - ${response.header.status} - " +
          s"${rh.method} ${rh.uri} - " +
          s"$procTime ms - " +
          s"${rh.host} - " +
          s"${rh.headers.get(HeaderNames.USER_AGENT).getOrElse("NA")}"
      )

      response.withHeaders("Processing-Time" -> procTime.toString)
    }
  }
}
