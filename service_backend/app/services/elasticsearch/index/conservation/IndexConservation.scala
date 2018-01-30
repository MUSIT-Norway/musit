package services.elasticsearch.index.conservation

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import models.elasticsearch._
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.FutureMusitResult
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticSearchConservationEventDao
import services.actor.ActorService
import services.conservation.ConservationProcessService
import services.elasticsearch.index.{IndexMaintainer, Indexer}

import scala.concurrent.{ExecutionContext, Future}

class IndexConservation @Inject()(
    elasticsearchEventDao: ElasticSearchConservationEventDao,
    override val indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    conservationProcessService: ConservationProcessService,
    override val client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
) extends Indexer {

  val logger = Logger(classOf[IndexConservation])

  override val indexAliasName: String = indexAlias

  override def createIndexMapping(
      indexName: String
  )(implicit ec: ExecutionContext): CreateIndexDefinition =
    ConservationIndexConfig.config(indexName)

  override def createElasticSearchBulkSource(
      config: IndexConfig,
      eventsAfter: Option[DateTime]
  )(
      implicit ec: ExecutionContext
  ): FutureMusitResult[Source[BulkCompatibleDefinition, NotUsed]] = {

    val futEventTypes = conservationProcessService.getAllEventTypes()

    futEventTypes.map { eventTypes =>
      val dbSource = elasticsearchEventDao.conservationEventStream(
        eventsAfter,
        Indexer.defaultFetchsize,
        eventTypes,
        elasticsearchEventDao.defaultEventProvider
      )
      createFlow(dbSource, config, actorService)
    }

  }

  /**
   * Split the database flow from one source and run them trough the matching pipeline.
   * When the pipeline is done it will be merged to a source that's sent to an
   * elasticsearch sink
   */
  private def createFlow(
      in: Source[MusitResult[ConservationSearch], NotUsed],
      config: IndexConfig,
      actorService: ActorService
  )(
      implicit ec: ExecutionContext
  ) = {

    val res = new ConservationTypeFlow(actorService, config)(ec)
    res.createFlow(in)
  }
}
