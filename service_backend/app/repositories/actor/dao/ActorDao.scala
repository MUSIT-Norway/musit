package repositories.actor.dao

import com.google.inject.{Inject, Singleton}
import models.actor.Person
import no.uio.musit.models.{ActorId, DatabaseId, MuseumId}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

@Singleton
class ActorDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  import profile.api._

  private val actorTable = TableQuery[ActorTable]

  def getByDbId(id: DatabaseId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.id === id).result.headOption)
  }

  def getByActorId(uuid: ActorId): Future[Option[Person]] = {
    val query = actorTable.filter { a =>
      a.applicationId === uuid || a.dpId === uuid
    }
    db.run(query.result.headOption)
  }

  def getByName(mid: MuseumId, searchString: String): Future[Seq[Person]] = {
    val likeArg = searchString.toLowerCase
    val query = actorTable.filter { a =>
      (a.fn.toLowerCase like s"%$likeArg%") && a.museumId === mid
    }.sortBy(_.fn)

    db.run(query.result)
  }

  def getByDataportenId(dataportenId: ActorId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.dpId === dataportenId).sortBy(_.fn).result.headOption)
  }

  def listBy(ids: Set[ActorId]): Future[Seq[Person]] = {
    db.run(actorTable.filter { a =>
      (a.applicationId inSet ids) || (a.dpId inSet ids)
    }.sortBy(_.fn).result)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(
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
