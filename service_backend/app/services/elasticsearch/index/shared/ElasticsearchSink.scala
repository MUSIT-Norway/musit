package services.elasticsearch.index.shared

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.bulk.BulkResponseItem
import com.sksamuel.elastic4s.streams.{
  BulkIndexingSubscriber,
  RequestBuilder,
  ResponseListener
}
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.Logger
import repositories.core.dao.IndexStatusDao
import services.elasticsearch.index.IndexMaintainer

import scala.concurrent.ExecutionContext

trait ElasticsearchSink {

  def client: HttpClient

  def onComplete: () => Unit

  def onError: Throwable => Unit

  def responseListener: ResponseListener[BulkCompatibleDefinition] =
    ResponseListener.noop

  private[this] implicit val rb = new RequestBuilder[BulkCompatibleDefinition] {
    override def request(t: BulkCompatibleDefinition) = t
  }

  def toElasticsearchSink(
      implicit as: ActorSystem
  ): Sink[BulkCompatibleDefinition, NotUsed] = {
    val sub: BulkIndexingSubscriber[BulkCompatibleDefinition] =
      client.subscriber[BulkCompatibleDefinition](
        completionFn = onComplete,
        errorFn = onError,
        listener = responseListener
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
    extends ElasticsearchSink
    with ResponseLogger {

  val logger              = Logger(classOf[DatabaseMaintainedElasticSearchIndexSink])
  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit = () => {
    if (indexCallback.notUsed) {
      logger.info(s"Indexing done for alias ${indexConfig.alias}")
      indexMaintainer.activateIndex(indexConfig.name, indexConfig.alias)
      indexStatusDao.indexed(indexConfig.alias, startTime)
      indexCallback.onSuccess(indexConfig)
    } else {
      logger.info(s"Indexing done with errors for alias ${indexConfig.alias}, s")
    }
  }

  override def onError: (Throwable) => Unit = { t =>
    logger.error(s"Indexing failed for alias ${indexConfig.alias}", t)
    indexCallback.onFailure(t)
  }

  override def responseListener = this
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
    extends ElasticsearchSink
    with ResponseLogger {

  val logger = Logger(classOf[DatabaseMaintainedElasticSearchUpdateIndexSink])

  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit =
    () => {
      if (!indexCallback.used) {
        indexMaintainer.indexNameForAlias(indexConfig.alias)
        indexStatusDao.update(indexConfig.alias, startTime)
        indexCallback.onSuccess(indexConfig)
      } else {
        logger.info(s"Indexing maybe done with errors for alias ${indexConfig.alias}")
      }
    }

  override def onError: (Throwable) => Unit =
    t => {
      logger.error(s"onError for alias ${indexConfig.alias} ${t.getMessage()}")

      indexCallback.onFailure(t)
    }

  override def responseListener = this
}

trait ResponseLogger extends ResponseListener[BulkCompatibleDefinition] {

  def logger: Logger

  override def onAck(resp: BulkResponseItem, org: BulkCompatibleDefinition): Unit = ()

  override def onFailure(
      resp: BulkResponseItem,
      org: BulkCompatibleDefinition
  ): Unit = {
    if (resp.error.isDefined) {
      logger.error(s"$resp")
    } else {
      logger.warn(s"Failure without error message: $resp")
    }
  }

}
