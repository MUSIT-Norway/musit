package services.elasticsearch.things

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch.{MustObjectSearch, MusitObjectSearch}
import models.musitobject.MusitObject
import repositories.elasticsearch.dao.ElasticsearchThingsDao
import services.elasticsearch.events.EventIndexConfig
import services.elasticsearch.{ElasticsearchFlow, IndexMaintainer, IndexName, Indexer}

import scala.concurrent.{ExecutionContext, Future}

class IndexMusitObjects @Inject()(
    elasticsearchThingsDao: ElasticsearchThingsDao,
    client: HttpClient,
    override val indexMaintainer: IndexMaintainer
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer[MusitObjectSearch] {

  override val indexAliasName    = "musit_objects"
  override val elasticsearchFlow = new ElasticsearchFlow(client, 1000)

  val populate =
    Flow[MusitObject].filter(_.uuid.isDefined).map(mObj => MustObjectSearch(mObj))

  override def toAction(indexName: IndexName) =
    Flow[MusitObjectSearch].map { thing =>
      indexInto(indexName.name, thing.documentType) id thing.documentId doc thing
    }

  def reindexAll(): Future[Done] = {
    val indexName = createIndexName()
    for {
      _    <- client.execute(EventIndexConfig.config(indexName.name))
      done <- indexMusitObjects(indexName)
    } yield done
  }

  private def indexMusitObjects(indexName: IndexName) = {
    elasticsearchThingsDao
      .objectStreams(20) // todo: make sources configurable
      .map(s => s.map(Source.fromPublisher))
      .flatMap { sources =>
        reindex(sources.map(_.via(populate)), Some(indexName))
      }
  }
}
