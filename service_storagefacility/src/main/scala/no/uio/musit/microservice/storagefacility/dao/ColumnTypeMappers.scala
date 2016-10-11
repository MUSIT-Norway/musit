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

package no.uio.musit.microservice.storagefacility.dao

import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.event.{EventId, EventTypeId}
import no.uio.musit.microservice.storagefacility.domain.storage.{StorageNodeId, StorageType}
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile

/**
 * Working with some of the DAOs require implicit mappers to/from strongly
 * typed value types/classes.
 */
trait ColumnTypeMappers {
  self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  implicit lazy val storageNodeIdMapper: BaseColumnType[StorageNodeId] =
    MappedColumnType.base[StorageNodeId, Long](
      snid => snid.underlying,
      longId => StorageNodeId(longId)
    )

  implicit lazy val storageTypeMapper =
    MappedColumnType.base[StorageType, String](
      storageType => storageType.entryName,
      string => StorageType.withName(string)
    )

  implicit lazy val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit lazy val eventTypeIdMapper: BaseColumnType[EventTypeId] =
    MappedColumnType.base[EventTypeId, Int](
      eventTypeId => eventTypeId.underlying,
      id => EventTypeId(id)
    )

  implicit lazy val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      museumId => museumId.underlying,
      id => MuseumId(id)
    )

  implicit lazy val nodePathMapper: BaseColumnType[NodePath] =
    MappedColumnType.base[NodePath, String](
      nodePath => nodePath.path,
      pathStr => NodePath(pathStr)
    )
}
