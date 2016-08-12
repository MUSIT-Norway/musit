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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dto.BaseEventDto
import no.uio.musit.microservice.event.service.{CustomFieldsSpec, CustomValuesInEventTable}
import no.uio.musit.microservices.common.linking.domain.Link

trait HasCustomField {
  val customFieldsSpec: CustomFieldsSpec
}

class Event(val baseEventProps: BaseEventDto) {
  val id: Option[Long] = baseEventProps.id
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType

  val relatedSubEvents = baseEventProps.relatedSubEvents

  def getCustomBool = CustomValuesInEventTable.getBool(this)

  def getCustomOptBool = CustomValuesInEventTable.getOptBool(this)

  def getCustomString = CustomValuesInEventTable.getString(this)

  def getCustomOptString = CustomValuesInEventTable.getOptString(this)

  def getCustomDouble = CustomValuesInEventTable.getDouble(this)

  def getCustomOptDouble = CustomValuesInEventTable.getOptDouble(this)

  def subEventsWithRelation(eventRelation: EventRelation) =
    relatedSubEvents.find(p => p.relation == eventRelation).map(_.events)

  // We assume none of the event-lists are empty. This is perhaps a wrong assumption.
  def hasSubEvents = relatedSubEvents.nonEmpty

  //Maybe not needed, just for convenience
  def getAllSubEvents = relatedSubEvents.flatMap(relatedEvents => relatedEvents.events)

  def getAllSubEventsAs[T] = getAllSubEvents.map(subEvent => subEvent.asInstanceOf[T])

}
