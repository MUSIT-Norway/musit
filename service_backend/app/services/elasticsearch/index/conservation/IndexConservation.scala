package services.elasticsearch.index.conservation

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.playjson._
import models.conservation.events.ConservationEvent
import models.elasticsearch._
import models.musitobject.MusitObject
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, EventTypeId, MuseumId}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.conservation.dao.ConservationProcessDao
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.{
  ElasticSearchConservationEventDao,
  ElasticsearchEventDao,
  ElasticsearchObjectsDao
}
import services.actor.ActorService
import services.elasticsearch.index.IndexerBase
//import services.elasticsearch.index
import services.elasticsearch.index.shared.{
  DatabaseMaintainedElasticSearchIndexSink,
  DatabaseMaintainedElasticSearchUpdateIndexSink
}
import services.elasticsearch.index.{IndexMaintainer, Indexer, TypeFlow}

import scala.concurrent.{ExecutionContext, Future}

class IndexConservation @Inject()(
    elasticsearchEventDao: ElasticSearchConservationEventDao,
    indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
) extends IndexerBase(indexStatusDao) {

  val logger = Logger(classOf[IndexConservation])

  override val indexAliasName: String = indexAlias

  override def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig] = {
    val config = createIndexConfig()
    client.execute(ConservationIndexConfig.config(config.name)).flatMap { res =>
      if (res.acknowledged) Future.successful(config)
      else Future.failed(new IllegalStateException("Unable to setup index"))
    }
  }

  override def reindexDocuments(indexCallback: IndexCallback, config: IndexConfig)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit = {

    val dbSource = elasticsearchEventDao.conservationEventStream(
      None,
      Indexer.defaultFetchsize,
      elasticsearchEventDao.defaultEventProvider
    )
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
      _.map { dt =>
        elasticsearchEventDao.conservationEventStream(
          Some(dt),
          Indexer.defaultFetchsize,
          elasticsearchEventDao.defaultEventProvider
        )
      }.getOrElse(Source.empty)
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
   * When the pipeline is done it will be merged to a source that's sent to an
   * elasticsearch sink
   */
  //: Flow[MusitResult[ConservationEvent], IndexDefinition, NotUsed]
  ///Creates a flow which logs and removes errors and puts the successful events into ES
  private def createFlow(
      in: Source[MusitResult[ConservationSearch], NotUsed],
      config: IndexConfig
  ) /*: Flow[MusitResult[ConservationEvent], BulkCompatibleDefinition, NotUsed]*/ = {

    val logAndRemoveErrorsFlow
      : Flow[MusitResult[ConservationSearch], ConservationSearch, NotUsed] =
      Flow[MusitResult[ConservationSearch]].map { element =>
        val res = element match {
          case x: MusitSuccess[ConservationSearch] =>
            x
          case err: MusitError =>
            println(s"ES indexing error: ${err.message}")
            logger.error(s"ES indexing error: ${err.message}")
            err
        }
        res
      }.collect {
        case mr: MusitSuccess[ConservationSearch] => mr.value
      }

    val intoEsFlow: Flow[ConservationSearch, BulkCompatibleDefinition, NotUsed] =
      Flow[ConservationSearch].map { thing =>
        val res =
          indexInto(config.name, conservationType).id(thing.event.id.get).doc(thing)
        //println(s"indexInto res:$res")
        res
      }

    in.via(logAndRemoveErrorsFlow.via(intoEsFlow))
  }
}
