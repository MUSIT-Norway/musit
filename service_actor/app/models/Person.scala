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

package models

import no.uio.musit.models.{ActorId, DatabaseId}
import no.uio.musit.security.AuthenticatedUser
import play.api.libs.json._

case class Person(
  id: Option[DatabaseId],
  fn: String,
  title: Option[String] = None,
  role: Option[String] = None,
  tel: Option[String] = None,
  web: Option[String] = None,
  email: Option[String] = None,
  dataportenId: Option[ActorId] = None,
  dataportenUser: Option[String] = None,
  applicationId: Option[ActorId] = None
)

object Person {
  val tupled = (Person.apply _).tupled
  implicit val format = Json.format[Person]

  def fromAuthUser(user: AuthenticatedUser): Person = {
    Person(
      id = None,
      fn = user.userInfo.name.getOrElse(""),
      email = user.userInfo.email,
      dataportenId = Option(user.userInfo.id),
      dataportenUser = user.userInfo.email
    )
  }
}

