package repositories.core.dao

import com.google.inject.{Inject, Singleton}
import models.elasticsearch.IndexStatus
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class IndexStatusDao @Inject()(
    implicit val dbConfigProvider: DatabaseConfigProvider,
    ec: ExecutionContext
) extends IndexStatusTable {

  val logger = Logger(classOf[IndexStatusDao])

  import profile.api._

  def logOnComplete[T](fut: Future[T], name: String) = {

    fut onComplete {
      case Success(s) => logger.info(s"<$name> success:" + s)
      case Failure(t) =>
        logger.info(s"<$name>: An error has occured: " + t.getMessage)
    }

  }

  def findLastIndexed(
      alias: String
  ): Future[MusitResult[Option[IndexStatus]]] = {
    logger.info("<findLastIndexed>")
    println("findLastIndexed") //Just in case logger is turned off by some means

    val query =
      esIndexStatusTable.filter(_.indexAlias === alias)
    val res = db
      .run(query.result.headOption)
      .map(optRow => MusitSuccess(optRow.map(fromRow)))
      .recover(nonFatal(s"Unable to find index status for alias $alias"))

    logOnComplete(res, "findLastIndexed")
    res
  }

  def indexed(
      alias: String,
      indexTime: DateTime
  ): Future[MusitResult[Unit]] = {
    logger.info("<indexed>")
    val res = db
      .run(esIndexStatusTable.insertOrUpdate((alias, indexTime, None)))
      .map {
        case 1     => MusitSuccess(())
        case other => MusitDbError(s"Expected to upsert one row got $other")
      }
      .recover(nonFatal(s"Unable to upsert index status for alias $alias"))

    logOnComplete(res, "indexed")
    res

  }

  def update(
      alias: String,
      updateTime: DateTime
  ): Future[MusitResult[Unit]] = {
    logger.info("<update>")
    val action =
      esIndexStatusTable
        .filter(_.indexAlias === alias)
        .map(_.indexUpdated)
        .update(Some(updateTime))

    val res = db
      .run(action)
      .map {
        case 1 => MusitSuccess(())
        case 0 => MusitDbError(s"No matching alias/index $alias")
      }
      .recover(nonFatal(s"Unable to update index status for alias $alias"))

    logOnComplete(res, "update")
    res
  }

  private def fromRow(row: (String, DateTime, Option[DateTime])) =
    IndexStatus(row._1, row._2, row._3)
}
