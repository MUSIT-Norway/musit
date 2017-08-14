package repositories.actor.dao

import models.actor.Person
import no.uio.musit.models.{ActorId, DatabaseId, MuseumId}
import play.api.db.slick.HasDatabaseConfigProvider
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

private[repositories] trait ActorTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  val actorTable = TableQuery[ActorTable]

  class ActorTable(
      tag: Tag
  ) extends Table[Person](tag, Some(MappingSchemaName), ActorTableName) {

    val id            = column[Option[DatabaseId]]("ACTORID", O.PrimaryKey, O.AutoInc)
    val fn            = column[String]("ACTORNAME")
    val dpId          = column[Option[ActorId]]("DATAPORTEN_UUID")
    val dpUsername    = column[Option[String]]("DATAPORTEN_USERNAME")
    val oldUsername   = column[Option[String]]("OLD_USERNAME")
    val oldPk         = column[Option[Int]]("LOKAL_PK")
    val oldTableId    = column[Option[Int]]("TABELLID")
    val oldSchemaName = column[Option[String]]("OLD_SCHEMANAME")
    val museumId      = column[Option[MuseumId]]("MUSEUM_ID")
    val applicationId = column[Option[ActorId]]("APPLICATION_UUID")

    val create = (
        id: Option[DatabaseId],
        fn: String,
        dataportenId: Option[ActorId],
        dataportenUsername: Option[String],
        applicationId: Option[ActorId]
    ) =>
      Person(
        id = id,
        fn = fn,
        dataportenId = dataportenId,
        dataportenUser = dataportenUsername,
        applicationId = applicationId
    )

    val destroy = (actor: Person) =>
      Some(
        (
          actor.id,
          actor.fn,
          actor.dataportenId,
          actor.dataportenUser,
          actor.applicationId
        )
    )

    // scalastyle:off method.name
    def * = (id, fn, dpId, dpUsername, applicationId) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}
