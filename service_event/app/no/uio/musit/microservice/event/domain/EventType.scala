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

import play.api.libs.json.{ JsResult, JsValue, Json }

sealed trait EventType {
  val id: Int
  val reads: (JsValue) => JsResult[Event]
  val typename: String
}

trait Companion[T] {
  type C
  def apply() : C
}

object Companion {
  implicit def companion[T](implicit comp : Companion[T]) = comp()
}

object TestCompanion {
  trait Foo

  case class Meh(d: String) extends Foo

  object Meh {
    def bar = "wibble"

    // Per-companion boilerplate for access via implicit resolution
    implicit def companion = new Companion[Meh] {
      type C = Meh.type
      def apply() = Meh
    }
  }

  import Companion._

  val fc = companion[Meh]  // Type is Foo.type
  val s = fc.bar          // bar is accessible
}

object EventType {

  val eventTypeByName: Map[String, EventType] = Map(
    MoveEvent.typename -> MoveEvent,
    ControlEvent.typename -> ControlEvent,
    ObservationEvent.typename -> ObservationEvent
  )

  val eventTypeById: Map[Int, EventType] = eventTypeByName.values.map(evt => evt.id -> evt).toMap
  val eventNameById: Map[Int, String] = eventTypeByName.values.map(evt => evt.id -> evt.typename).toMap
  val eventIdByName: Map[String, Int] = eventTypeByName.values.map(evt => evt.typename -> evt.id).toMap

  def getId(name: String) = eventIdByName.get(name).get
  def getName(id: Int) = eventNameById.get(id).get
  def apply(stType: String) = eventTypeByName.get(stType)
  def apply(id: Int) = eventTypeById.get(id)
}

object MoveEvent extends EventType {
  val id = 1
  val reads = Move.format.reads _
  val typename: String = "move"
}

object ControlEvent extends EventType {
  val id = 2
  val reads = Control.format.reads _
  val typename: String = "control"
}

object ObservationEvent extends EventType {
  val id = 3
  val reads = Observation.format.reads _
  val typename: String = "observation"
}
