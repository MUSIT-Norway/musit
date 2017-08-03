package repositories.shared.dao

import no.uio.musit.models._
import play.api.db.slick.HasDatabaseConfigProvider
import repositories.storage.dao.SchemaNameOpt
import slick.jdbc.JdbcProfile

private[repositories] trait SharedTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val localObjectsTable = TableQuery[LocalObjectsTable]

  type LocalObjectRow = (ObjectUUID, EventId, StorageNodeId, MuseumId, Option[String])

  class LocalObjectsTable(
      tag: Tag
  ) extends Table[LocalObjectRow](tag, SchemaNameOpt, "NEW_LOCAL_OBJECT") {
    // scalastyle:off method.name
    def * =
      (
        objectUuid,
        latestMoveId,
        currentLocationId,
        museumId,
        objectType
      )

    // scalastyle:on method.name

    val objectUuid        = column[ObjectUUID]("OBJECT_UUID", O.PrimaryKey)
    val latestMoveId      = column[EventId]("LATEST_MOVE_ID")
    val currentLocationId = column[StorageNodeId]("CURRENT_LOCATION_ID")
    val museumId          = column[MuseumId]("MUSEUM_ID")
    val objectType        = column[Option[String]]("OBJECT_TYPE")

  }

}
