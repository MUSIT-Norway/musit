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

package repositories.dao

import java.util.UUID

import no.uio.musit.models.{ActorId, AuthId, DatabaseId, OrgId}
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile

trait ColumnTypeMappers {
  self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  implicit val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, Long](
      aid => aid.underlying,
      longId => ActorId(longId)
    )

  implicit val orgIdMapper: BaseColumnType[OrgId] =
    MappedColumnType.base[OrgId, Long](
      oid => oid.underlying,
      longId => OrgId(longId)
    )

  implicit val dbIdMapper: BaseColumnType[DatabaseId] =
    MappedColumnType.base[DatabaseId, Long](
      did => did.underlying,
      longId => DatabaseId(longId)
    )

  implicit val authIdMapper: BaseColumnType[AuthId] =
    MappedColumnType.base[AuthId, String](
      aid => aid.asString,
      strId => AuthId(UUID.fromString(strId))
    )

}
