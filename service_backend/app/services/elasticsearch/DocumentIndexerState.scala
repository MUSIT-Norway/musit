package services.elasticsearch

import akka.stream.Materializer
import play.api.Logger
import services.elasticsearch.DocumentIndexer._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DocumentIndexerState[S](
    indexer: Indexer[S],
    indexMaintainer: IndexMaintainer
) extends DocumentIndexer {

  val logger = Logger(indexer.getClass)

  var reindexStatus: DocumentIndexerStatus     = NotExecuted
  var updateIndexStatus: DocumentIndexerStatus = NotExecuted

  var indexName: Option[IndexName] = None

  override def initIndex()(implicit ec: ExecutionContext): Future[Boolean] = {
    val aliasExists = indexMaintainer.indexNameForAlias(indexer.indexAliasName)
    aliasExists.foreach(optName => indexName = optName.map(IndexName.apply))
    aliasExists.map(_.isDefined)
  }

  override def reindex()(implicit ec: ExecutionContext, mat: Materializer): Unit = {
    logger.info("reindex starting")
    reindexStatus = Executing
    indexer.reindexToNewIndex().onComplete {
      case Success(nextIndexName) =>
        indexName = Some(nextIndexName)
        reindexStatus = IndexSuccess
        logger.info("reindex done")
      case Failure(t) =>
        reindexStatus = IndexFailed
        logger.warn("reindex failed", t)
    }
  }

  override def updateIndex()(implicit ec: ExecutionContext, mat: Materializer): Unit = {
    logger.info("updateIndex, not implemented")
    updateIndexStatus = Executing
    indexName
      .map(indexer.updateExistingIndex)
      .getOrElse(Future.successful(()))
      .onComplete {
        case Success(_) =>
          reindexStatus = IndexSuccess
          logger.info("updating index done")
        case Failure(t) =>
          reindexStatus = IndexFailed
          logger.warn("update index failed", t)
      }
  }
}

object DocumentIndexerState {
  def apply[S](i: Indexer[S], m: IndexMaintainer) = new DocumentIndexerState[S](i, m)
}
