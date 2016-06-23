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

import play.api.libs.json.{JsObject, JsResult}

class EventType(val id: Int, name: String, fromJsonToEvent: (EventType, JsObject) => JsResult[Event]) {

  val eventTypes = Seq(
    new EventType(1, "Move", Move.fromJson),
    new EventType(2, "Control", Control.fromJson),
    new EventType(3, "Observation", Observation.fromJson)
    // Add new event type here....
  )

  val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase)
  def getById(id: Int) = eventTypeById.get(id)
}