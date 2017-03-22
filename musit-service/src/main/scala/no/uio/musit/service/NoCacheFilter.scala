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

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.{HeaderNames, Status}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class NoCacheFilter @Inject()(
    implicit val mat: Materializer
) extends Filter {

  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next(rh).map(
      response =>
        response.header.status match {
          case Status.NOT_MODIFIED =>
            response

          case _ =>
            response.withHeaders(
              HeaderNames.CACHE_CONTROL -> "no-cache,no-store,max-age=0"
            )
      }
    )
  }
}
