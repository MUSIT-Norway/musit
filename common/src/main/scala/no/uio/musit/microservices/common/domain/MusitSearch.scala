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
package no.uio.musit.microservices.common.domain

case class MusitSearch(searchMap: Map[String, String], searchStrings: List[String])

object MusitSearch {

  def parseParams(p: List[String]): Map[String, String] =
    p.foldLeft(Map[String, String]())((acc, next) => next.split("=") match {
      case Array(key, value) if value.nonEmpty =>
        acc + (key -> value)
      case other => throw new IllegalArgumentException(s"Syntax error in (part of) search part of URL: $next")
    })

  def parseSearch(search: String): MusitSearch =
    "^\\[(.*)\\]$".r.findFirstIn(search) match {
      case Some(string) =>
        val indices = Indices.getFrom(string)
        MusitSearch(
          parseParams(indices.filter(_.contains('='))),
          indices.filterNot(_.contains("="))
        )
      case _ =>
        MusitSearch(Map(), List())
    }

  implicit val queryBinder = new BindableOf[MusitSearch](_.map(v => Right(parseSearch(v.trim))))
}
