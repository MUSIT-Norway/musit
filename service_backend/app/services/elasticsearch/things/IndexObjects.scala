package services.elasticsearch.things

import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch.{IndexConfig, MusitObjectSearch}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.Configuration
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchThingsDao
import services.elasticsearch._
import services.elasticsearch.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

import scala.concurrent.{ExecutionContext, Future}

class IndexObjects @Inject()(
    elasticsearchThingsDao: ElasticsearchThingsDao,
    indexStatusDao: IndexStatusDao,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer[MusitObjectSearch] {

  private[this] val esBathSize: Int =
    cfg.getInt("musit.elasticsearch.streams.musitObjects.esBatchSize").getOrElse(1000)
  private[this] val concurrentSources =
    cfg.getInt("musit.elasticsearch.streams.musitObjects.concurrentSources").getOrElse(20)
  private[this] val fetchSize =
    cfg
      .getInt("musit.elasticsearch.streams.musitObjects.dbStreamFetchSize")
      .getOrElse(1000)

  override val indexAliasName = "musit_objects"

  override def reindexToNewIndex(indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    val indexName = createIndexName()
    val config =
      IndexConfig(
        indexName.name,
        indexAliasName,
        MusitObjectsIndexConfig.config(indexName.name)
      )

    elasticsearchThingsDao.objectStreams(concurrentSources, fetchSize).map { sources =>
      val es = new DatabaseMaintainedElasticSearchIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        config,
        indexCallback
      ).toElasticsearchSink

      val musitObjectFlow = new MusitObjectTypeFlow().flow(config)

      val esBulkSource = Source.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val mergeToEs = builder.add(Merge[BulkCompatibleDefinition](sources.size))
        sources.map(_.via(musitObjectFlow)).foreach(_ ~> mergeToEs)
        SourceShape.of(mergeToEs.out)
      })

      esBulkSource.runWith(es)
    }
  }

  override def updateExistingIndex(indexName: IndexName, indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {
    val config =
      IndexConfig(
        indexName.name,
        indexAliasName,
        MusitObjectsIndexConfig.config(indexName.name)
      )
    findLastIndexDateTime().map { mdt =>
      mdt.map { dt =>
        elasticsearchThingsDao.objectsChangedAfterTimstampStream(fetchSize, dt)
      }.getOrElse(Source.empty)
    }.map(source => {
      val es = new DatabaseMaintainedElasticSearchUpdateIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        config,
        indexCallback
      ).toElasticsearchSink
      val musitObjectFlow = new MusitObjectTypeFlow().flow(config)
      source.via(musitObjectFlow).runWith(es)
    })
  }

  private def findLastIndexDateTime(): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case err: MusitError => None
    }
  }

}
