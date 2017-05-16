package repositories.actor.dao

import java.util.UUID

import no.uio.musit.models.{ActorId, DatabaseId, MuseumId, OrgId}
import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile

trait ColumnTypeMappers { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.api._

  implicit val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      aid => aid.asString,
      strId => ActorId(UUID.fromString(strId))
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
  implicit val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      mid => mid.underlying,
      intId => MuseumId.fromInt(intId)
    )
}
