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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._

sealed trait Event extends BaseMusitDomain {
  val id: Option[Long]
  val links: Option[Seq[Link]]
  val eventType: String
  val note: Option[String]
}

object Event {
  implicit val format = Json.format[Move]
}

case class DefaultEvent(
  id: Option[Long],
  links: Option[Seq[Link]],
  eventType: String,
  note: Option[String]
) extends Event

case class Move(
  id: Option[Long],
  links: Option[Seq[Link]],
  eventType: String,
  note: Option[String]
) extends Event

object Move {
  implicit val format = Json.format[Move]
}

case class Observation(
  id: Option[Long],
  links: Option[Seq[Link]],
  eventType: String,
  note: Option[String]
) extends Event

object Observation {
  implicit val format = Json.format[Observation]
}

case class Control(
  id: Option[Long],
  links: Option[Seq[Link]],
  eventType: String,
  note: Option[String]
) extends Event

object Control {
  implicit val format = Json.format[Control]
}