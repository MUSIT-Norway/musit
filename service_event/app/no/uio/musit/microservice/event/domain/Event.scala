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
import play.api.libs.json.{ JsObject, Json }

/**
 * Created by jstabel on 6/10/16.
 */

case class AtomLink(rel: String, href: String) {
  def toLink(localId: Long) = Link(None, Some(localId), rel, href)

}
case class EventInfo(id: Option[Long], eventType: String, eventData: Option[JsObject], links: Option[Seq[AtomLink]])

case class Event(id: Option[Long], eventTypeId: Int, note: Option[String]) {

  def eventType = {
    EventType.eventTypeIdToEventType(eventTypeId)
  }

  def asSeq[T](optSeq: Option[Seq[T]]) = optSeq.getOrElse(Seq.empty[T])

  //def allAtomLinks = asSeq(actors) // Todo: Add places, artefacts etc.
}

trait EventExtension

case class CompleteEvent(baseEvent: Event, eventExtension: Option[EventExtension], links: Option[Seq[AtomLink]]) {

}

object CompleteEvent {

}

object AtomLink {
  def tupled = (AtomLink.apply _).tupled

  implicit val format = Json.format[AtomLink]

  def createFromLink(link: Link) = AtomLink(link.rel, link.href)
}

object EventInfo {
  def tupled = (EventInfo.apply _).tupled

  implicit val format = Json.format[EventInfo]
}

