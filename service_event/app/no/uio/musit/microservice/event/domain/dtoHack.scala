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

import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.Json

/* Quick and dirty way to get the eventType to be the name instead of the integer id... Can and ought to be improved!*/
case class BaseEventDTOHack(id: Option[Long], links: Option[Seq[Link]], eventType: String, note: Option[String])

object BaseEventDTOHack {
  def fromBaseEventDto(baseEvent: BaseEventDTO) = {
    val eventTypeName = (EventType.getById(baseEvent.eventType).get).name
    BaseEventDTOHack(baseEvent.id, baseEvent.links, eventTypeName, baseEvent.note)
  }

  implicit val format = Json.format[BaseEventDTOHack]
}

