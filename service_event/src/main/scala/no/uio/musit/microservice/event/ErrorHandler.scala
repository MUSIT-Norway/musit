/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.event;

import no.uio.musit.microservices.common.domain.{MusitError, MusitException}
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Logger.warn(s"ErrorHandler - Client error ($statusCode): $message")
    Future.successful(
      Status(statusCode)(Json.toJson(MusitError(statusCode, message)))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {

    def serverError = {
      Logger.error("ErrorHandler - Server error", exception)
      Future.successful(
        InternalServerError(Json.toJson(MusitError(play.api.http.Status.INTERNAL_SERVER_ERROR, exception.getMessage)))
      )
    }

    exception match {
      case e: MusitException => {
        if(e.isClientError) onClientError(request, e.status, e.getMessage)
        else
          serverError
      }
      case _: Throwable => {
        serverError
      }
    }
  }
}