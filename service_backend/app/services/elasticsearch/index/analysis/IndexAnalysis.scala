package services.elasticsearch.index.analysis

import akka.NotUsed
import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import models.elasticsearch._
import no.uio.musit.functional.FutureMusitResult
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.{ElasticsearchEventDao, ElasticsearchObjectsDao}
import services.actor.ActorService
import services.elasticsearch.index.{IndexMaintainer, Indexer, TypeFlow}

import scala.concurrent.ExecutionContext

/**
 * Index documents into the events index
 */
class IndexAnalysis @Inject()(
    elasticsearchEventDao: ElasticsearchEventDao,
    elasticsearchObjectsDao: ElasticsearchObjectsDao,
    override val indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    override val client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
) extends Indexer {

  val logger = Logger(classOf[IndexAnalysis])

  override val indexAliasName: String = indexAlias

  override def createIndexMapping(
      indexName: String
  )(implicit ec: ExecutionContext): CreateIndexDefinition =
    AnalysisIndexConfig.config(indexName)

  override def createElasticSearchBulkSource(
      config: IndexConfig,
      eventsAfter: Option[DateTime]
  )(
      implicit ec: ExecutionContext
  ): FutureMusitResult[Source[BulkCompatibleDefinition, NotUsed]] = {
    val dbSource = elasticsearchEventDao.analysisEventsStream(eventsAfter)
    FutureMusitResult.successful(createFlow(dbSource, config))
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
        createFlowType(
          config,
          new AnalysisTypeFlow(actorService, elasticsearchObjectsDao)
        ) {
          case a: AnalysisSearchType => a
        },
        createFlowType(
          config,
          new SampleCreatedTypeFlow(actorService, elasticsearchObjectsDao)
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

}
