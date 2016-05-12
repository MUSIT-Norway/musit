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

package no.uio.musit.microservices.common.linking.domain

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

/* Domain classes */
case class Link(id: Long, localTableId: Long, rel: String, href: String)

/* Helper singletons */
object Link {
  def tupled = (Link.apply _).tupled

  implicit val linkWrites = new Writes[Link] {
    override def writes(link: Link): JsValue = Json.obj(
      "rel" -> link.rel,
      "href" -> link.href
    )
  }

  def applyLink(rel: String, href: String): Link = Link(-1, -1, rel, href)

  implicit val linkReads: Reads[Link] = (
    (JsPath \ "rel").read[String](minLength[String](1)) and
    (JsPath \ "href").read[String](minLength[String](1))
  )(applyLink _)
}