package services.elasticsearch.index.events

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.{ElasticsearchEventDao, ElasticsearchThingsDao}
import services.actor.ActorService
import services.elasticsearch.index.{IndexMaintainer, Indexer, TypeFlow}
import services.elasticsearch.index.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Index documents into the events index
 */
class IndexEvents @Inject()(
    analysisEventsExportDao: ElasticsearchEventDao,
    elasticsearchThingsDao: ElasticsearchThingsDao,
    indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
) extends Indexer {

  val logger = Logger(classOf[IndexEvents])

  override val indexAliasName: String = indexAlias

  override def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig] = {
    val config = createIndexConfig()
    client.execute(EventIndexConfig.config(config.name)).flatMap { res =>
      if (res.acknowledged) Future.successful(config)
      else Future.failed(new IllegalStateException("Unable to setup index"))
    }
  }

  override def reindexDocuments(indexCallback: IndexCallback, config: IndexConfig)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    val dbSource     = analysisEventsExportDao.analysisEventsStream()
    val esBulkSource = createFlow(dbSource, config)

    val es = new DatabaseMaintainedElasticSearchIndexSink(
      client,
      indexMaintainer,
      indexStatusDao,
      config,
      indexCallback
    ).toElasticsearchSink

    esBulkSource.runWith(es)
  }

  override def updateExistingIndex(
      indexConfig: IndexConfig,
      indexCallback: IndexCallback
  )(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {
    findLastIndexDateTime().map {
      _.map(dt => analysisEventsExportDao.analysisEventsStream(Some(dt)))
        .getOrElse(Source.empty)
    }.map { dbSource =>
      val esBulkSource = createFlow(dbSource, indexConfig)

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

  /**
   * Split the database flow from one source and run them trough the matching pipeline.
   * When the pipeline is done it will be merged to a source that's send to a
   * elasticsearch sink
   */
  private def createFlow(
      in: Source[AnalysisModuleEventSearch, NotUsed],
      config: IndexConfig
  )(implicit ec: ExecutionContext) = {
    val analysisFlowTypes =
      List(
        createFlowType(config, new AnalysisCollectionTypeFlow(actorService)) {
          case a: AnalysisCollectionSearchType => a
        },
        createFlowType(config, new AnalysisTypeFlow(actorService, elasticsearchThingsDao)) {
          case a: AnalysisSearchType => a
        },
        createFlowType(
          config,
          new SampleCreatedTypeFlow(actorService, elasticsearchThingsDao)
        ) {
          case a: SampleCreatedEventSearchType => a
        }
      )
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val flowCount = analysisFlowTypes.size

      val flowBC    = builder.add(Broadcast[AnalysisModuleEventSearch](flowCount))
      val mergeToEs = builder.add(Merge[BulkCompatibleDefinition](flowCount))

      in ~> flowBC
      analysisFlowTypes.foreach(f => flowBC ~> f ~> mergeToEs)

      SourceShape.of(mergeToEs.out)
    })
  }

  private def createFlowType[I, D <: Searchable](
      config: IndexConfig,
      typeFlow: TypeFlow[I, D]
  )(
      pf: PartialFunction[AnalysisModuleEventSearch, I]
  ) = {
    Flow[AnalysisModuleEventSearch].collect[I](pf).via(typeFlow.flow(config))
  }

  private def findLastIndexDateTime()(
      implicit ec: ExecutionContext
  ): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case _: MusitError   => None
    }
  }

}
