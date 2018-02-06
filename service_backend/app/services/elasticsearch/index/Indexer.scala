package services.elasticsearch.index

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import org.joda.time.DateTime
import play.api.Logger
import repositories.core.dao.IndexStatusDao
import services.elasticsearch.index.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

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
 * The Indexer instance will be managed by the {{{IndexProcessor}}} through the
 * {{{ElasticsearchService}}}. This is because it need to be executed on it's own
 * actor system to isolate it from the rest of the application.
 */
trait Indexer {

  /**
   * The index alias name.
   */
  val indexAliasName: String

  val indexMaintainer: IndexMaintainer

  val indexStatusDao: IndexStatusDao

  val client: HttpClient

  def createIndexMapping(indexName: String)(
      implicit ec: ExecutionContext
  ): CreateIndexDefinition

  /**Create a source (flow), which we will put into ES.
   We need future here because object-indexing needs that.
   */
  def createElasticSearchBulkSource(
      config: IndexConfig,
      eventsAfter: Option[DateTime]
  )(
      implicit ec: ExecutionContext
  ): FutureMusitResult[Source[BulkCompatibleDefinition, NotUsed]]

  /**
   * Create a new index with the correct mapping
   */
  def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig] = {
    val config  = createIndexConfig()
    val mapping = createIndexMapping(config.name)
    client.execute(mapping).flatMap { res =>
      if (res.acknowledged) Future.successful(config)
      else Future.failed(new IllegalStateException("Unable to setup index"))
    }
  }

  /**
   * Reindex all documents
   */
  def reindexDocuments(indexConfig: IndexConfig, indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    val futEsBulkSource = createElasticSearchBulkSource(indexConfig, None)

    futEsBulkSource.map { esBulkSource =>
      val es = new DatabaseMaintainedElasticSearchIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        indexConfig,
        indexCallback
      ).toElasticsearchSink
      esBulkSource.runWith(es)
    }
  }

  /**
   * Update the existing index with updated and new documents
   */
  def updateExistingIndex(indexConfig: IndexConfig, indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    findLastIndexDateTime().map {
      _.map { dt =>
        val futEsBulkSource = createElasticSearchBulkSource(indexConfig, Some(dt))
        futEsBulkSource.map { esBulkSource =>
          val es = new DatabaseMaintainedElasticSearchUpdateIndexSink(
            client,
            indexMaintainer,
            indexStatusDao,
            indexConfig,
            indexCallback
          ).toElasticsearchSink

          esBulkSource.runWith(es)
        }
      }
    }.map { _ =>
      () // A hack to get rid of a compiler warning:
    // ("a type was inferred to be `Any`; this may indicate a programming error.")
    }.recover {
      //Får ikke så lett tak i et logger objekt, ellers ville vi gjort:
      // logger.error(s"Unable to update existing index: ${indexConfig.name}, error: ${error.getMessage()}")

      case NonFatal(t) => indexCallback.onFailure(t)
    }

  }

  /**
   * Reindex all documents to index with a new fresh index.
   */
  final def reindexToNewIndex(indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {
    createIndex().map(reindexDocuments(_, indexCallback)).recover {
      case NonFatal(t) => indexCallback.onFailure(t)
    }
  }

  /**
   * The actual index that we will use. We will hide this behind an alias. That's why
   * we prefix it with the alias name.
   */
  protected def createIndexConfig(): IndexConfig =
    IndexConfig(s"${indexAliasName}_${System.currentTimeMillis()}", indexAliasName)

  protected def findLastIndexDateTime()(
      implicit ec: ExecutionContext
  ): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case _: MusitError   => None
    }
  }
}

object Indexer {
  val defaultBatchSize              = 1000
  val defaultConcurrentSourcesCount = 20
  val defaultFetchsize              = 1000
}
