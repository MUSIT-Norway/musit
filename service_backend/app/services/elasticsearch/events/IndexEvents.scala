package services.elasticsearch.events

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchEventDao
import services.actor.ActorService
import services.elasticsearch._
import services.elasticsearch.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}

import scala.concurrent.{ExecutionContext, Future}

class IndexEvents @Inject()(
    analysisEventsExportDao: ElasticsearchEventDao,
    indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
) extends Indexer[EventSearch] {

  val logger = Logger(classOf[IndexEvents])

  override val indexAliasName = "events"

  override def reindexToNewIndex()(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Future[IndexName] = {
    val indexName = createIndexName()
    val config =
      IndexConfig(indexName.name, indexAliasName, EventIndexConfig.config(indexName.name))

    val dbSource     = analysisEventsExportDao.analysisEventsStream()
    val esBulkSource = createFlow(dbSource, config)

    val es = new DatabaseMaintainedElasticSearchIndexSink(
      client,
      indexMaintainer,
      indexStatusDao,
      config.alias
    ).toElasticsearchSink

    esBulkSource.runWith(es)
    //todo callback or something when done
    Future.successful(indexName)
  }

  override def updateExistingIndex(indexName: IndexName)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Future[Unit] = {
    findLastIndexDateTime().map {
      _.map(dt => analysisEventsExportDao.analysisEventsStream(Some(dt)))
        .getOrElse(Source.empty)
    }.flatMap { dbSource =>
      val config =
        IndexConfig(
          indexName.name,
          indexAliasName,
          EventIndexConfig.config(indexName.name)
        )
      val esBulkSource = createFlow(dbSource, config)

      val es = new DatabaseMaintainedElasticSearchUpdateIndexSink(
        client,
        indexMaintainer,
        indexStatusDao,
        config.alias
      ).toElasticsearchSink

      esBulkSource.runWith(es)
      //todo callback or something when done
      Future.successful(())
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
        createFlowType(config, new AnalysisTypeFlow(actorService)) {
          case a: AnalysisSearchType => a
        },
        createFlowType(config, new SampleCreatedTypeFlow(actorService)) {
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

  private def createFlowType[I, D](config: IndexConfig, typeFlow: TypeFlow[I, D])(
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
