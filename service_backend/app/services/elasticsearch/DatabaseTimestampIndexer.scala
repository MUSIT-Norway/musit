package services.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import repositories.core.dao.IndexStatusDao
import services.elasticsearch.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.ExecutionContext

class DatabaseTimestampIndexer(
    client: HttpClient,
    indexMaintainer: IndexMaintainer,
    indexStatusDao: IndexStatusDao
) {

  def index[Inn](cfg: IndexConfig, s: Source[BulkCompatibleDefinition, NotUsed])(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: ActorMaterializer
  ) = {
    client.execute(cfg.mapping).foreach { _ =>
      val sink = new DatabaseMaintainedElasticSearchIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        cfg.alias
      )
      s.runWith(sink.toElasticsearchSink(as))
    }

  }

  def update[Inn](cfg: IndexConfig, s: Source[BulkCompatibleDefinition, NotUsed])(
      implicit ec: ExecutionContext,
      as: ActorSystem,
      mat: ActorMaterializer
  ) = {
    client.execute(cfg.mapping).foreach { _ =>
      val sink = new DatabaseMaintainedElasticSearchUpdateIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        cfg.alias
      )
      s.runWith(sink.toElasticsearchSink(as))
    }
  }

}

//todo move this class to modules or so. Find a better name for it also
case class IndexConfig(indexName: String, alias: String, mapping: CreateIndexDefinition)
