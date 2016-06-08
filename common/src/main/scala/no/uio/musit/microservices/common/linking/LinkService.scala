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
package no.uio.musit.microservices.common.linking

import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.linking.domain.Link

object LinkService {
  val baseUrl = "http://localhost:7070"

  def self(uri: String): Link = {
    Link(None, None, "self", baseUrl + uri)
  }

  def self(uri: String, id:Option[Long]):Either[MusitError, Link] = {
    id match {
      case Some(realId) => Right(self(s"$uri$realId"))
      case None => Left(MusitError(400, s"Reference id of link ($uri) was not a value"))
    }
  }

  def local(key: Option[Long], rel: String, uri: String): Link = {
    Link(None, key, rel, baseUrl + uri)
  }

  def local(key: Option[Long], rel: String, uri: String, id:Option[Long]): Either[MusitError, Link] = {
    id match {
      case Some(realId) => Right(local(key, rel, s"$uri$realId"))
      case None => Left(MusitError(400, s"Reference id of link ($uri) was not a value"))
    }
  }
}
