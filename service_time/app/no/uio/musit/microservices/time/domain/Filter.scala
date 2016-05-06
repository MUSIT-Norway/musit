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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.mvc.QueryStringBindable
import scala.util.Try

case class MusitError(message: String)

object MusitError {
  implicit val format: Format[MusitError] = Json.format[MusitError]
}

case class MusitFilter(filters: Seq[String])

object MusitFilter {

  implicit def bindableFilter = new QueryStringBindable[MusitFilter] {
    def parseFilter(filter: String): Seq[String] =
      "^\\[(.*)\\]$".r.findFirstIn(filter) match {
        case Some(str) => str.stripPrefix("[").stripSuffix("]").split(",").sorted.toList
        case None      => List()
      }

    override def bind(key: String, data: Map[String, Seq[String]]): Option[Either[String, MusitFilter]] =
      data.get(key).map { v => Right(MusitFilter(parseFilter(v.head))) }

    override def unbind(key: String, mf: MusitFilter) = {
      "filter=[" + mf.filters.reduce(_ + "," + _) + "]"
    }
  }
}

case class MusitSearch(searchMap: Map[String, String], searchStrings: List[String])

object MusitSearch {

  implicit def bindableFilter = new QueryStringBindable[MusitSearch] {
    
    def parseParam(s: String) = s.split("=") match {
      case Array(key, value) if value.trim.nonEmpty =>
        (key -> value)
      case Array(key, emptyValue) =>
        throw new IllegalArgumentException("Invalid search parameter: " + key + "=" + emptyValue)
      case Array(key) =>
        throw new IllegalArgumentException("Invalid search parameter: " + key + "=n/a")
    }

    def parseValue(string: String) = {
      val list = string.stripPrefix("[").stripSuffix("]").split(",").sorted.toList
      val searchStrings = list.filterNot(_.contains("="))
      val keyvalStrings = list.filter(_.contains('='))
      val paramMap = keyvalStrings.foldLeft(Map[String, String]())((acc, next) => acc + parseParam(next))
      MusitSearch(paramMap, searchStrings)
    }
    
    def parseSearch(filter: String) =
      "^\\[(.*)\\]$".r.findFirstIn(filter) match {
        case Some(str) => {
          try {
            Right(parseValue(str))
          } catch {
            case e: IllegalArgumentException => Left(e.getLocalizedMessage)
          }
        }
        case other => Left("Invalid search query: " + other)
      }

    override def bind(key: String, data: Map[String, Seq[String]]) =
      data.get(key).map(_.head).map(parseSearch(_))

    override def unbind(key: String, mf: MusitSearch) = {
      "filter=[" + mf.searchMap.map(kv => kv._1 + "=" + kv._2).reduce(_ + "," + _) + "," + mf.searchStrings.reduce(_ + ", " + _) + "]"
    }
  }
}