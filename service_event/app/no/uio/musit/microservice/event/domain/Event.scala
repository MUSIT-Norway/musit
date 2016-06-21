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
import play.api.libs.json.{JsObject, JsValue, Json}

/**
 * Created by jstabel on 6/10/16.
 */

case class AtomLink(rel: String, href: String) {
  def toLink(localId: Long) = Link(None, Some(localId), rel, href)

}

/**
 *
 * @param id
 * @param eventType meant to already be validated
 * @param eventData
 * @param links
 */

case class EventInfo(id: Option[Long], eventType: String, eventData: Option[Event], links: Option[Seq[AtomLink]])

case class Event(
  override val id: Option[Long],
  override val links: Option[Seq[Link]],
  eventTypeId: Int,
  note: Option[String]
) extends BaseMusitDomain {

  def eventType = EventType.apply(eventTypeId)

  def asSeq[T](optSeq: Option[Seq[T]]) = optSeq.getOrElse(Seq.empty[T])

  //def allAtomLinks = asSeq(actors) // Todo: Add places, artifacts etc.
}

sealed trait EventExtension

object EventExtension {
  def apply(eventType: String) : Option[EventExtension] = {
    eventType match {
      case "kontroll" => Some(Kontroll())
      case other => None
    }
  }
}

case class Kontroll(
  override val id: Option[Long],
  override val links: Option[Seq[Link]],
  override val eventTypeId: Int,
  override val note: Option[String]
)  extends Event(id, links, eventTypeId, note)

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
  def apply(json: JsValue) = {
    EventInfo(
      None,
      (json \ "eventType").as[String],
      (json \ "eventData").toOption.map(_.as[JsObject]),
      (json \ "links").asOpt[List[AtomLink]]
    )
  }
}

