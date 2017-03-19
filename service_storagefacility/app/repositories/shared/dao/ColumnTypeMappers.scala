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

package repositories.shared.dao

import java.util.UUID
import java.sql.{Timestamp => JSqlTimestamp}

import models.storage.event.EventTypeId
import models.storage.nodes.StorageType
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.time.DefaultTimezone
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.JdbcProfile

/**
 * Working with some of the DAOs require implicit mappers to/from strongly
 * typed value types/classes.
 */
trait ColumnTypeMappers { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.api._

  implicit val storageNodeDbIdMapper: BaseColumnType[StorageNodeDatabaseId] =
    MappedColumnType.base[StorageNodeDatabaseId, Long](
      snid => snid.underlying,
      longId => StorageNodeDatabaseId(longId)
    )

  implicit val storageNodeIdMapper: BaseColumnType[StorageNodeId] =
    MappedColumnType.base[StorageNodeId, String](
      sid => sid.asString,
      strId => StorageNodeId(UUID.fromString(strId))
    )

  implicit val objectIdMapper: BaseColumnType[ObjectId] =
    MappedColumnType.base[ObjectId, Long](
      oid => oid.underlying,
      longId => ObjectId(longId)
    )

  implicit val objectUuidMapper: BaseColumnType[ObjectUUID] =
    MappedColumnType.base[ObjectUUID, String](
      oid => oid.asString,
      strId => ObjectUUID.unsafeFromString(strId)
    )

  implicit val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      aid => aid.asString,
      strId => ActorId(UUID.fromString(strId))
    )

  implicit val storageTypeMapper =
    MappedColumnType.base[StorageType, String](
      storageType => storageType.entryName,
      string => StorageType.withName(string)
    )

  implicit val eventTypeIdMapper: BaseColumnType[EventTypeId] =
    MappedColumnType.base[EventTypeId, Int](
      eventTypeId => eventTypeId.underlying,
      id => EventTypeId(id)
    )

  implicit val objTypeMapper: BaseColumnType[ObjectType] =
    MappedColumnType.base[ObjectType, String](
      tpe => tpe.name,
      str => ObjectType.unsafeFromString(str)
    )

  implicit val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      museumId => museumId.underlying,
      id => MuseumId(id)
    )

  implicit val nodePathMapper: BaseColumnType[NodePath] =
    MappedColumnType.base[NodePath, String](
      nodePath => nodePath.path,
      pathStr => NodePath(pathStr)
    )

  implicit val dateTimeMapper: BaseColumnType[DateTime] =
    MappedColumnType.base[DateTime, JSqlTimestamp](
      dt => new JSqlTimestamp(dt.getMillis),
      jt => new DateTime(jt, DefaultTimezone)
    )

  implicit val jsonMapper: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](
      jsv => Json.prettyPrint(jsv),
      str => Json.parse(str)
    )
}
