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

sealed trait RelationStorageStrategy

case object RelationTableStrategy extends RelationStorageStrategy
case object PartOfStrategy extends RelationStorageStrategy

case class EventRelation(name: String, inverseName: String, isLinksDirection: Boolean, storageStrategy: RelationStorageStrategy = RelationTableStrategy) {
  def getWithLinksDirection = if(isLinksDirection) this else EventRelations.getByNameOrFail(this.inverseName)
}


object EventRelations {
    private def defRel(name: String, inverseName: String, storageStrategy: RelationStorageStrategy = RelationTableStrategy) = EventRelation(name, inverseName, true, storageStrategy)
    private val relations = Seq(
      defRel("parts", "part_of", PartOfStrategy),
      defRel("motivates", "motivated_by")
    )

  private val bothSidesRelations = relations ++ relations.map(rel=> EventRelation(rel.inverseName, rel.name, !(rel.isLinksDirection), rel.storageStrategy))


  private val relationByName: Map[String, EventRelation] = bothSidesRelations.map(rel => rel.name.toLowerCase -> rel).toMap

  //This one is hardcoded some places in the system, because it is treated in a special way (not stored in the event-relation-table, but in the base event-table directly
  val relation_parts = getByNameOrFail("parts")

  def getByName(name: String) = relationByName.get(name.toLowerCase)

  def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find relation with name : $name")
  def getMusitResultByName(name: String) = getByName(name).toMusitResult(ErrorHelper.badRequest(s"Unable to find relation with name : $name"))
}