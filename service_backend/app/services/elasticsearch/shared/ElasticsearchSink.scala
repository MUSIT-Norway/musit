package services.elasticsearch.shared

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.streams.BulkIndexingSubscriber
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.streams.RequestBuilder
import models.elasticsearch.{IndexCallback, IndexConfig, IndexName}
import no.uio.musit.time
import org.joda.time.DateTime
import repositories.core.dao.IndexStatusDao
import services.elasticsearch.IndexMaintainer

import scala.concurrent.ExecutionContext

trait ElasticsearchSink {

  def client: HttpClient

  def onComplete: () => Unit
  def onError: Throwable => Unit

  private[this] implicit val rb = new RequestBuilder[BulkCompatibleDefinition] {
    override def request(t: BulkCompatibleDefinition) = t
  }

  def toElasticsearchSink(
      implicit as: ActorSystem
  ): Sink[BulkCompatibleDefinition, NotUsed] = {
    val sub: BulkIndexingSubscriber[BulkCompatibleDefinition] =
      client.subscriber[BulkCompatibleDefinition](
        completionFn = onComplete,
        errorFn = onError
      )
    Sink.fromSubscriber(sub)
  }

}

/**
 * Used to reindex a fresh index. It will swap the index when done.
 */
class DatabaseMaintainedElasticSearchIndexSink(
    val client: HttpClient,
    indexMaintainer: IndexMaintainer,
    indexStatusDao: IndexStatusDao,
    indexConfig: IndexConfig,
    indexCallback: IndexCallback
)(implicit ec: ExecutionContext)
    extends ElasticsearchSink {

  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit = () => {
    indexMaintainer.activateIndex(indexConfig.indexName, indexConfig.alias)
    indexStatusDao.indexed(indexConfig.alias, startTime)
    indexCallback.success(IndexName(indexConfig.indexName))
  }

  override def onError: (Throwable) => Unit =
    _ => indexCallback.failure()

}

/**
 * Used when updating an existing index.
 */
class DatabaseMaintainedElasticSearchUpdateIndexSink(
    val client: HttpClient,
    indexMaintainer: IndexMaintainer,
    indexStatusDao: IndexStatusDao,
    indexConfig: IndexConfig,
    indexCallback: IndexCallback
)(implicit ec: ExecutionContext)
    extends ElasticsearchSink {

  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit =
    () => {
      indexMaintainer.indexNameForAlias(indexConfig.alias)
      indexStatusDao.update(indexConfig.alias, startTime)
      indexCallback.success(IndexName(indexConfig.indexName))
    }

  override def onError: (Throwable) => Unit =
    _ => indexCallback.failure()
}
