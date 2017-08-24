package services.elasticsearch.things

import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.Configuration
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchThingsDao
import services.actor.ActorService
import services.elasticsearch._
import services.elasticsearch.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Index documents into the musit_object index.
 */
class IndexObjects @Inject()(
    elasticsearchThingsDao: ElasticsearchThingsDao,
    indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer {

  private[this] val esBathSize: Int =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.esBatchSize")
      .getOrElse(1000)
  private[this] val concurrentSources =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.concurrentSources")
      .getOrElse(20)
  private[this] val fetchSize =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.dbStreamFetchSize")
      .getOrElse(1000)

  override val indexAliasName: String = indexAlias

  override def createIndex()(implicit ec: ExecutionContext) = {
    val config = createIndexConfig()
    client.execute(MusitObjectsIndexConfig.config(config.name)).map(_ => config)
  }

  override def reindexDocuments(indexCallback: IndexCallback, config: IndexConfig)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    val sampleSourceFuture = Future.successful(
      elasticsearchThingsDao.sampleStream(fetchSize, None)
    )
    val sources = for {
      objSources   <- elasticsearchThingsDao.objectStreams(concurrentSources, fetchSize)
      sampleSource <- sampleSourceFuture
    } yield (objSources, sampleSource)

    sources.map {
      case (objSources, sampleSource) =>
        val es = new DatabaseMaintainedElasticSearchIndexSink(
          client,
          indexMaintainer,
          indexStatusDao,
          config,
          indexCallback
        ).toElasticsearchSink

        val musitObjectFlow  = new MusitObjectTypeFlow().flow(config)
        val sampleObjectFlow = new SampleTypeFlow(actorService).flow(config)

        val esBulkSource = Source.fromGraph(GraphDSL.create() { implicit builder =>
          import GraphDSL.Implicits._

          val mergeToEs =
            builder.add(Merge[BulkCompatibleDefinition](objSources.size + 1))

          objSources.map(_.via(musitObjectFlow)).foreach(_ ~> mergeToEs)
          sampleSource.via(sampleObjectFlow) ~> mergeToEs

          SourceShape.of(mergeToEs.out)
        })

        esBulkSource.runWith(es)
    }
  }

  override def updateExistingIndex(
      indexConfig: IndexConfig,
      indexCallback: IndexCallback
  )(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {
    findLastIndexDateTime().map { mdt =>
      mdt.map { dt =>
        elasticsearchThingsDao.objectsChangedAfterTimestampStream(fetchSize, dt)
      }.getOrElse(Source.empty)
    }.map(source => {
      val es = new DatabaseMaintainedElasticSearchUpdateIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        indexConfig,
        indexCallback
      ).toElasticsearchSink
      val musitObjectFlow = new MusitObjectTypeFlow().flow(indexConfig)
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
