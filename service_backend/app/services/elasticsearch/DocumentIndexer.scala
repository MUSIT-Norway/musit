package services.elasticsearch

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

trait DocumentIndexer {
  import services.elasticsearch.DocumentIndexer.DocumentIndexerStatus

  /**
   * Check the index status. Return true if the index exists.
   */
  def initIndex()(implicit ec: ExecutionContext): Future[Boolean]

  /**
   * The status for the update index
   */
  def updateIndexStatus(): DocumentIndexerStatus

  /**
   * The status for the reindexing
   */
  def reindexStatus(): DocumentIndexerStatus

  /**
   * Create a new index and reindex all the documents to it.
   */
  def reindex()(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: Materializer
  ): Unit

  /**
   * Update the current index with the latest documents
   */
  def updateIndex()(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: Materializer
  ): Unit

}

object DocumentIndexer {

  /**
   * Statuses for the index. The ready flag indicate that we can
   * call reindex/updateIndex
   */
  sealed abstract class DocumentIndexerStatus(val ready: Boolean)
  object NotExecuted  extends DocumentIndexerStatus(true)
  object Executing    extends DocumentIndexerStatus(false)
  object IndexSuccess extends DocumentIndexerStatus(true)
  object IndexFailed  extends DocumentIndexerStatus(false)
}
