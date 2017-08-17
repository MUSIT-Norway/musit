package repositories.core.dao

import com.google.inject.{Inject, Singleton}
import models.elasticsearch.IndexStatus
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class IndexStatusDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends IndexStatusTable {

  import profile.api._

  def findLastIndexed(
      alias: String
  ): Future[MusitResult[Option[IndexStatus]]] = {
    val query =
      esIndexStatusTable.filter(_.indexAlias === alias)
    db.run(query.result.headOption)
      .map(optRow => MusitSuccess(optRow.map(fromRow)))
      .recover(nonFatal(s"Unable to find index status for alias $alias"))
  }

  def indexed(
      alias: String,
      indexTime: DateTime
  ): Future[MusitResult[Unit]] =
    db.run(esIndexStatusTable.insertOrUpdate((alias, indexTime, None)))
      .map {
        case 1     => MusitSuccess(())
        case other => MusitDbError(s"Expected to upsert one row got $other")
      }
      .recover(nonFatal(s"Unable to upsert index status for alias $alias"))

  def update(
      alias: String,
      updateTime: DateTime
  ): Future[MusitResult[Unit]] = {
    val action =
      esIndexStatusTable
        .filter(_.indexAlias === alias)
        .map(_.indexUpdated)
        .update(Some(updateTime))

    db.run(action)
      .map {
        case 1 => MusitSuccess(())
        case 0 => MusitDbError(s"No matching alias/index $alias")
      }
      .recover(nonFatal(s"Unable to update index status for alias $alias"))

  }

  private def fromRow(row: (String, DateTime, Option[DateTime])) =
    IndexStatus(row._1, row._2, row._3)

}
