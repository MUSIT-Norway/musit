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
package no.uio.musit.microservices.time.domain

case class MusitFilter(filters: List[String])

object MusitFilter {

  def parseFilter(filter: String): Either[String, MusitFilter] =
    "^\\[(.*)\\]$".r.findFirstIn(filter) match {
      case Some(string) if string.nonEmpty =>
        Right(MusitFilter(Indices.getFrom(string)))
      case _ =>
        Right(MusitFilter(List()))
    }

  implicit val queryBinder = new BindableOf[MusitFilter](_.map(v => parseFilter(v.trim)))
}
