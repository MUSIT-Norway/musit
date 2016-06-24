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

package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.MusitError
import play.api.http.Status

import scala.concurrent.Future

/**
 * Created by jstabel on 6/7/16.
 */
object ErrorHelper {
  def badRequest(text: String, devMessage: String = "") = MusitError(Status.BAD_REQUEST, text, devMessage)
  def notFound(text: String, devMessage: String = "") = MusitError(Status.NOT_FOUND, text, devMessage)
  def conflict(text: String, devMessage: String = "") = MusitError(Status.CONFLICT, text, devMessage)
  def notImplemented(text: String, devMessage: String = "") = MusitError(Status.NOT_IMPLEMENTED, text, devMessage)

  def futureNotImplemented(text: String, devMessage: String = "") = Future.successful(Left(notImplemented(text, devMessage)))
}
