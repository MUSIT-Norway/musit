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

package no.uio.musit.models

import play.api.libs.json.{Format, Json}

/**
 * A NodePath contains a comma separated String of StorageNodeId (or Long)
 * values, each of these ID's can be represented as a NamedPathElement.
 *
 * @param nodeId StorageNodeId of the named path element
 * @param name   String containing the name value of the StorageNode.
 */
case class NamedPathElement(nodeId: StorageNodeDatabaseId, name: String)

object NamedPathElement {

  implicit val formats: Format[NamedPathElement] = Json.format[NamedPathElement]

}
