package services.elasticsearch

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Logger
import services.elasticsearch.DocumentIndexer._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DocumentIndexerState[S](
    indexer: Indexer[S],
    indexMaintainer: IndexMaintainer
) extends DocumentIndexer {

  val logger = Logger(classOf[DocumentIndexerState[_]])

  var reindexStatus: DocumentIndexerStatus     = NotExecuted
  var updateIndexStatus: DocumentIndexerStatus = NotExecuted

  var indexName: Option[IndexName] = None

  override def initIndex()(implicit ec: ExecutionContext): Future[Boolean] = {
    val aliasExists = indexMaintainer.indexNameForAlias(indexer.indexAliasName)
    aliasExists.foreach(optName => indexName = optName.map(IndexName.apply))
    aliasExists.map(_.isDefined)
  }

  override def reindex()(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: Materializer
  ): Unit = {
    logger.info(s"reindex starting for alias ${indexer.indexAliasName}")
    reindexStatus = Executing
    indexer.reindexToNewIndex().onComplete {
      case Success(nextIndexName) =>
        indexName = Some(nextIndexName)
        reindexStatus = IndexSuccess
        logger.info(s"reindex of alias ${indexer.indexAliasName} is done")
      case Failure(t) =>
        reindexStatus = IndexFailed
        logger.warn(s"reindex of alias ${indexer.indexAliasName} has failed", t)
    }
  }

  override def updateIndex()(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: Materializer
  ): Unit = {
    logger.info(s"starting updating index for alias ${indexer.indexAliasName} ")
    updateIndexStatus = Executing
    indexName
      .map(indexer.updateExistingIndex)
      .getOrElse(Future.successful(()))
      .onComplete {
        case Success(_) =>
          reindexStatus = IndexSuccess
          logger.info(s"updating alias ${indexer.indexAliasName} is done")
        case Failure(t) =>
          reindexStatus = IndexFailed
          logger.warn(s"updating alias ${indexer.indexAliasName} has failed", t)
      }
  }
}

object DocumentIndexerState {
  def apply[S](i: Indexer[S], m: IndexMaintainer) = new DocumentIndexerState[S](i, m)
}
