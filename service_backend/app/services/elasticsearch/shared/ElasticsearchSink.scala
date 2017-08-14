package services.elasticsearch.shared

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.streams.BulkIndexingSubscriber
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.streams.RequestBuilder
import no.uio.musit.time
import org.joda.time.DateTime
import repositories.core.dao.IndexStatusDao
import services.elasticsearch.IndexMaintainer

import scala.concurrent.ExecutionContext

trait ElasticsearchSink {

  def client: HttpClient

  def onComplete: () => Unit

  private[this] implicit val rb = new RequestBuilder[BulkCompatibleDefinition] {
    override def request(t: BulkCompatibleDefinition) = t
  }

  def toElasticsearchSink(
      implicit as: ActorSystem
  ): Sink[BulkCompatibleDefinition, NotUsed] = {
    val sub: BulkIndexingSubscriber[BulkCompatibleDefinition] =
      client.subscriber[BulkCompatibleDefinition](
        completionFn = onComplete
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
    alias: String
)(implicit ec: ExecutionContext)
    extends ElasticsearchSink {

  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit = () => {
    indexMaintainer.indexNameForAlias(alias)
    indexStatusDao.indexed(alias, startTime)
  }

}

/**
 * Used when updating an existing index.
 */
class DatabaseMaintainedElasticSearchUpdateIndexSink(
    val client: HttpClient,
    indexMaintainer: IndexMaintainer,
    indexStatusDao: IndexStatusDao,
    alias: String
)(implicit ec: ExecutionContext)
    extends ElasticsearchSink {

  val startTime: DateTime = time.dateTimeNow

  override def onComplete: () => Unit = () => {
    indexMaintainer.indexNameForAlias(alias)
    indexStatusDao.update(alias, startTime)
  }

}
