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

package no.uio.musit.security

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.{Reads, __}
import play.api.mvc.Request

import scala.util.Try

/**
 * Value class providing a wrapper around a bearer token String.
 *
 * @param underlying String value representation of the bearer token
 */
case class BearerToken(underlying: String) extends AnyVal {

  def asHeader: (String, String) = (AUTHORIZATION, BearerToken.prefix + underlying)

}

object BearerToken {

  implicit val reads: Reads[BearerToken] = __.read[String].map(BearerToken.apply)

  val prefix = "Bearer "

  /**
   * Function to assist in extracting the BearerToken from incoming requests.
   *
   * @param request The incoming request
   * @tparam A The body content type of the incoming request
   * @return Option of BearerToken
   */
  def fromRequestHeader[A](request: Request[A]): Option[BearerToken] = {
    request.headers
      .get(AUTHORIZATION)
      .find(_.startsWith(prefix))
      .flatMap { headerValue =>
        Try(headerValue.substring(prefix.length)).toOption.map { token =>
          BearerToken(token)
        }
      }
  }

}
