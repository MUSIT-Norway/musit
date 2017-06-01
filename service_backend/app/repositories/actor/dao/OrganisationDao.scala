package repositories.actor.dao

import com.google.inject.{Inject, Singleton}
import models.actor.{Organisation, WordList}
import no.uio.musit.MusitResults._
import no.uio.musit.models.OrgId
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.shared.dao.{ColumnTypeMappers, DbErrorHandlers}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

@Singleton
class OrganisationDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with DbErrorHandlers
    with ColumnTypeMappers {

  import profile.api._

  val logger = Logger(classOf[OrganisationDao])

  private val orgTable = TableQuery[OrganisationTable]

  def getById(id: OrgId): Future[Option[Organisation]] = {
    db.run(orgTable.filter(_.id === id).result.headOption)
  }

  def getByNameAndTags(searchName: String, tag: String): Future[Seq[Organisation]] = {
    val query = orgTable.filter { org =>
      (org.serviceTags like s"%|$tag|%") &&
      ((org.fullName like s"%$searchName%") ||
      (org.synonyms like s"%|$searchName|%"))
    }
    db.run(query.result)
  }

  def getByName(searchString: String): Future[Seq[Organisation]] = {
    val query = orgTable.filter { org =>
      (org.fullName like s"%$searchString%") || (org.synonyms like s"%|$searchString|%")
    }
    db.run(query.result)
  }

  def insert(organization: Organisation): Future[Organisation] = {
    val query = orgTable returning
      orgTable.map(_.id) into ((organization, id) => organization.copy(id = Some(id)))

    val action = query += organization

    db.run(action)
  }

  def update(org: Organisation): Future[MusitResult[Option[Int]]] = {
    // "Record was updated!"
    val query = orgTable.filter(_.id === org.id).update(org)
    db.run(query).map {
      case upd: Int if upd == 0 => MusitSuccess(None)
      case upd: Int if upd == 1 => MusitSuccess(Some(upd))
      case upd: Int if upd > 1  => MusitDbError("Too many records were updated")
    }
  }

  def delete(id: OrgId): Future[Int] = {
    db.run(orgTable.filter(_.id === id).delete)
  }

  def getAnalysisLabList: Future[MusitResult[Seq[Organisation]]] = {
    db.run(orgTable.filter(_.serviceTags like "%analysis%").result)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"An unexpected error occurred fetching lab list"))
  }

  private class OrganisationTable(
      tag: Tag
  ) extends Table[Organisation](tag, Some(SchemaName), OrgTableName) {

    val id          = column[OrgId]("ORG_ID", O.PrimaryKey, O.AutoInc)
    val fullName    = column[String]("FULL_NAME")
    val tel         = column[Option[String]]("TEL")
    val web         = column[Option[String]]("WEB")
    val synonyms    = column[Option[String]]("SYNONYMS")
    val serviceTags = column[Option[String]]("SERVICE_TAGS")

    val create = (
        id: Option[OrgId],
        fullName: String,
        tel: Option[String],
        web: Option[String],
        synonyms: Option[String],
        serviceTags: Option[String]
    ) =>
      Organisation(
        id,
        fullName,
        tel,
        web,
        WordList.fromOptDbString(synonyms),
        WordList.fromOptDbString(serviceTags)
    )

    val destroy = (org: Organisation) =>
      Option(
        (
          org.id,
          org.fullName,
          org.tel,
          org.web,
          org.synonyms.map(_.asDbString),
          org.serviceTags.map(_.asDbString)
        )
    )

    // scalastyle:off method.name
    def * = (id.?, fullName, tel, web, synonyms, serviceTags) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}
