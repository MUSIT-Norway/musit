package services.elasticsearch.index.objects

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import models.analysis.SampleObject
import models.elasticsearch.{IndexCallback, IndexConfig}
import models.musitobject.MusitObject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.Configuration
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchObjectsDao
import services.actor.ActorService
import services.elasticsearch.index.{IndexMaintainer, Indexer}
import services.elasticsearch.index.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Index documents into the musit_object index.
 */
class IndexObjects @Inject()(
    elasticsearchObjectsDao: ElasticsearchObjectsDao,
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
    println("TEMP: createIndex (Objects)")
    val config = createIndexConfig()
    client.execute(MusitObjectsIndexConfig.config(config.name)).flatMap { res =>
      if (res.acknowledged) Future.successful(config)
      else Future.failed(new IllegalStateException("Unable to setup index"))
    }
  }

  override def reindexDocuments(indexCallback: IndexCallback, config: IndexConfig)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    println("TEMP: reindexDocuments (Objects)")

    val sampleSourceFuture = Future.successful(
      elasticsearchObjectsDao.sampleStream(fetchSize, None)
    )
    val sources = for {
      objSources   <- elasticsearchObjectsDao.objectStreams(concurrentSources, fetchSize)
      sampleSource <- sampleSourceFuture
    } yield (objSources, sampleSource)

    println("før source.map")
    sources.map {
      case (objSources, sampleSource) =>
        val es = new DatabaseMaintainedElasticSearchIndexSink(
          client,
          indexMaintainer,
          indexStatusDao,
          config,
          indexCallback
        ).toElasticsearchSink

        val esBulkSource = createFlow(config, objSources, sampleSource)
        println("etter createFlow")
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
        val objectSource =
          elasticsearchObjectsDao.objectsChangedAfterTimestampStream(fetchSize, dt)
        val sampleSource = elasticsearchObjectsDao.sampleStream(fetchSize, Some(dt))
        (objectSource, sampleSource)
      }.getOrElse((Source.empty, Source.empty))
    }.map {
      case (objectSource, sampleSource) => {
        val es = new DatabaseMaintainedElasticSearchUpdateIndexSink(
          client,
          indexMaintainer,
          indexStatusDao,
          indexConfig,
          indexCallback
        ).toElasticsearchSink

        val esBulkSource = createFlow(indexConfig, Seq(objectSource), sampleSource)
        esBulkSource.runWith(es)
      }
    }
  }

  private def createFlow(
      config: IndexConfig,
      objSources: Seq[Source[MusitObject, NotUsed]],
      sampleSource: Source[SampleObject, NotUsed]
  ) = {
    val musitObjectFlow  = new MusitObjectTypeFlow().flow(config)
    val sampleObjectFlow = new SampleTypeFlow(actorService).flow(config)

    println(s"TEMP: Creating flow, objSources.size=${objSources.size}")

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val mergeToEs =
        builder.add(Merge[BulkCompatibleDefinition](objSources.size + 1))

      objSources.map(_.via(musitObjectFlow)).foreach(_ ~> mergeToEs)
      sampleSource.via(sampleObjectFlow) ~> mergeToEs

      SourceShape.of(mergeToEs.out)
    })
  }

  private def findLastIndexDateTime(): Future[Option[DateTime]] = {
    println(s"TEMP: Calling findLastIndexDateTime on index $indexAliasName")

    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case err: MusitError => None
    }
  }

}
