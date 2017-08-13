package repositories.elasticsearch.dao

import no.uio.musit.repositories.BaseColumnTypeMappers
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

trait IndexStatusTable
    extends HasDatabaseConfigProvider[JdbcProfile]
    with BaseColumnTypeMappers {

  import profile.api._

  val SchemaName             = "MUSARK_CORE"
  val EsIndexStatusTableName = "ES_INDEX_STATUS"

  val esIndexStatusTable = TableQuery[EsIndexStatusTable]

  type EsIndexStatusRow = (String, DateTime, Option[DateTime])

  class EsIndexStatusTable(
      val tag: Tag
  ) extends Table[EsIndexStatusRow](tag, Some(SchemaName), EsIndexStatusTableName) {

    val indexAlias   = column[String]("INDEX_ALIAS", O.PrimaryKey)
    val indexCreated = column[DateTime]("INDEX_CREATED")
    val indexUpdated = column[Option[DateTime]]("INDEX_UPDATED")

    override def * = (indexAlias, indexCreated, indexUpdated)
  }

}
