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

import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper

/**
 * Created by jstabel on 7/6/16.
 */


//isNormalizedDirection is whether this direction is the same which the links go in the event_relation_event table (from -> to).
case class EventRelation(id: Int, name: String, inverseName: String, isNormalized: Boolean) {

  def getNormalizedDirection = if (isNormalized) this else EventRelations.getByNameOrFail(this.inverseName)
}

object EventRelations {
  private def defRel(id: Int, name: String, inverseName: String) = EventRelation(id, name, inverseName, true)
  private val relations = Seq(
    defRel(1, "parts", "part_of"),
    defRel(2, "motivates", "motivated_by")
  )

  private val bothSidesRelations = relations ++ relations.map(rel => EventRelation(rel.id, rel.inverseName, rel.name, !(rel.isNormalized)))

  private val relationByName: Map[String, EventRelation] = bothSidesRelations.map(rel => rel.name.toLowerCase -> rel).toMap

  //Note that this one is deliberately one-sided, we only want to find the one in the "proper" direction when searching by id. Else we need separate ids for the reverse relations
  private val relationById: Map[Int, EventRelation] = relations.map(rel => rel.id -> rel).toMap

  //This one is hardcoded some places in the system, because it is treated in a special way (not stored in the event-relation-table, but in the base event-table directly
  val relation_parts = getByNameOrFail("parts")

  //Shouldn't be used in the main framework, but perhaps used by tests and some other logic
  val relation_motivates = getByNameOrFail("motivates")

  def getByName(name: String) = relationByName.get(name.toLowerCase)

  def getById(id: Int) = relationById.get(id)
  def getByIdOrFail(id: Int) = getById(id).getOrFail(s"Unable to find relation with id: $id")

  def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find relation with name : $name")
  def getMusitResultByName(name: String) = getByName(name).toMusitResult(ErrorHelper.badRequest(s"Unable to find relation with name : $name"))
}