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

import com.google.inject.Singleton
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

// $COVERAGE-OFF$
@Singleton
class ErrorHandler extends HttpErrorHandler {

  val logger = Logger(classOf[ErrorHandler])

  def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] = {
    logger.warn(s"ErrorHandler - Client error ($statusCode): $message")
    Future.successful(
      Status(statusCode)(Json.obj("status" -> statusCode, "message" -> message))
    )
  }

  def onServerError(
      request: RequestHeader,
      exception: Throwable
  ): Future[Result] = {
    logger.error("ErrorHandler - Server error", exception)
    Future.successful(
      InternalServerError(Json.obj("status" -> 500, "message" -> exception.getMessage))
    )
  }
}
// $COVERAGE-ON$
