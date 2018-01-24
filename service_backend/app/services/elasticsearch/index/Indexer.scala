package services.elasticsearch.index

import akka.actor.ActorSystem
import akka.stream.Materializer
import models.elasticsearch.{IndexCallback, IndexConfig}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Indexer is where the actual indexing work is done. It has the ability to create a new
 * index and reindex an existing index.
 *
 * Typical steps for reindexing is the following:
 *  - Create a new index with elastic search mappings settings
 *  - Stream the documents that should be indexed to elasticsearch
 *  - Maintain the index by swapping the alias to the new index
 *  - Report back the status trough the callback when done
 *
 * Typical steps for updating the index:
 *  - Stream new and updated documents to elasticsearch
 *  - Report back the status trough the callback when done
 *
 * Each index has often more then one type in elasticsearch. Each type is implemented as
 * a akka-stream flow step of {{{TypeFlow}}}. This is because the sources will be setup
 * differently based on if it's a reindex or update of the index. The flow step must be
 * the same regardless of how the {{{Source}}} is created.
 *
 * The Indexer instance will be managed by the {{{IndexProcessor}}} trough the
 * {{{ElasticsearchService}}}. This is because it need to be executed on it's own
 * actor system to isolate it from the rest of the application.
 */
trait Indexer {

  /**
   * The index alias name.
   */
  val indexAliasName: String

  /**
   * Swaps the old alias with the new index.
   */
  val indexMaintainer: IndexMaintainer

  /**
   * Create a new index with the correct mapping
   */
  def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig]

  def reindexDocuments(indexCallback: IndexCallback, config: IndexConfig)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit

  /**
   * Update the existing index with updated and new documents
   */
  def updateExistingIndex(index: IndexConfig, indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit

  /**
   * Reindex all documents to index with a new fresh index.
   */
  final def reindexToNewIndex(indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {
    createIndex().map(reindexDocuments(indexCallback, _)).recover {
      case NonFatal(t) => indexCallback.onFailure(t)
    }
  }

  /**
   * Tha actual index that we will use. We will hide this behind an alias. That's why
   * we prefix it with the alias name.
   */
  protected def createIndexConfig(): IndexConfig =
    IndexConfig(s"${indexAliasName}_${System.currentTimeMillis()}", indexAliasName)

}

object Indexer {
  val defaultBatchSize              = 1000
  val defaultConcurrentSourcesCount = 20
  val defaultFetchsize              = 1000
}
