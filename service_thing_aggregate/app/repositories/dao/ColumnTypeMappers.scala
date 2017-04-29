package repositories.dao

import java.util.UUID

import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfig
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
      str => StorageNodeId.unsafeFromString(str)
    )

  implicit val objectIdMapper: BaseColumnType[ObjectId] =
    MappedColumnType.base[ObjectId, Long](
      oid => oid.underlying,
      longId => ObjectId(longId)
    )

  implicit val objectUuidMapper: BaseColumnType[ObjectUUID] =
    MappedColumnType.base[ObjectUUID, String](
      oid => oid.asString,
      str => ObjectUUID.unsafeFromString(str)
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

  implicit val museumNoMapper: BaseColumnType[MuseumNo] =
    MappedColumnType.base[MuseumNo, String](
      museumNo => museumNo.value,
      noStr => MuseumNo(noStr)
    )

  implicit val subNoMapper: BaseColumnType[SubNo] =
    MappedColumnType.base[SubNo, String](
      subNo => subNo.value,
      noStr => SubNo(noStr)
    )
}
