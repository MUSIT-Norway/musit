package services.elasticsearch.index.objects

import akka.NotUsed
import akka.stream.scaladsl.{GraphDSL, Merge, Source}
import akka.stream.{Materializer, SourceShape}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import models.analysis.SampleObject
import models.elasticsearch.IndexConfig
import models.musitobject.MusitObject
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.functional.Extensions._
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchObjectsDao
import repositories.musitsearchobject.dao.MusitSearchObjectDao
import services.actor.ActorService
import services.elasticsearch.index.{IndexMaintainer, Indexer}
import slick.jdbc.{ResultSetConcurrency, ResultSetType}
//import repositories.musitsearchobject.dao.MusitSearchObjectDao
//import repositories.musitsearchobject.dao.MusitSearchObjectDao.SearchObjectTable

import scala.concurrent.{ExecutionContext, Future}

/**
 * Index documents into the musit_object index.
 */
class IndexObjects @Inject()(
    elasticsearchObjectsDao: ElasticsearchObjectsDao,
    searchObjectDao: MusitSearchObjectDao,
    override val indexStatusDao: IndexStatusDao,
    actorService: ActorService,
    override val client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer,
    val dbConfigProvider: DatabaseConfigProvider

//val searchObjectDao: MusitSearchObjectDao
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer
    /*with MusitSearchObjectDao*/ {

  private val logger = Logger(classOf[IndexObjects])

  private[this] val esBathSize: Int =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.esBatchSize")
      .getOrElse(Indexer.defaultBatchSize)
  private[this] val concurrentSources =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.concurrentSources")
      .getOrElse(Indexer.defaultConcurrentSourcesCount)
  private[this] val fetchSize =
    cfg
      .getOptional[Int]("musit.elasticsearch.streams.musitObjects.dbStreamFetchSize")
      .getOrElse(Indexer.defaultFetchsize)

  override val indexAliasName: String = indexAlias

  override def createIndexMapping(
      indexName: String
  )(implicit ec: ExecutionContext): CreateIndexDefinition =
    MusitObjectsIndexConfig.config(indexName)

  override def maybeUpdateDbSearchTable(dt: DateTime): Future[Unit] = {
    logger.info("<maybeUpdateDbSearchTable>")

    val res =
      try {
        searchObjectDao.updateSearchTable(dt).recover {
          case err: Throwable =>
            logger.error(s"updating of search table failed: ${err.getMessage()}")
            ()
        }
      } catch {
        case err: Exception => {
          logger.error(
            s"Exception: updating of search table (maybeUpdateDbSearchTable) failed: ${err.getMessage()}"
          )

        }
        Future.successful(())

      }

    res.onComplete(_ => logger.info("</maybeUpdateDbSearchTable>"))
    res

  }

  override def maybeRecreateDbSearchTable(): Future[Unit] = {
    var res = searchObjectDao.recreateSearchTable().recover {
      case err: Throwable =>
        logger.error(s"recreating of search table failed: ${err.getMessage()}")
        ()
    }

    //val res = searchObjectDao.recreateSearchTable()

    /*Todo: Samples?
    val sampleSource =
      elasticsearchObjectsDao.sampleStream(fetchSize, None)
    val objSources = elasticsearchObjectsDao.objectStreams(concurrentSources, fetchSize)
     */

    res

  }

  override def createElasticSearchBulkSource(
      config: IndexConfig,
      eventsAfter: Option[DateTime]
  )(
      implicit ec: ExecutionContext
  ): FutureMusitResult[Source[BulkCompatibleDefinition, NotUsed]] = {
    val sampleSource =
      elasticsearchObjectsDao.sampleStream(fetchSize, eventsAfter)

    //We do quite different things on a full reindex vs update for objects
    val res = eventsAfter match {
      case Some(dt) =>
        val objectSource =
          elasticsearchObjectsDao.objectsChangedAfterTimestampStream(fetchSize, dt)

        Future.successful(createFlow(config, Seq(objectSource), sampleSource))

      case None => {
        for {
          objSources <- elasticsearchObjectsDao
                         .objectStreams(concurrentSources, fetchSize)
          localSampleSource <- Future.successful(sampleSource)

        } yield createFlow(config, objSources, localSampleSource)
      }
    }
    res.toMusitFuture()
  }

  private def createFlow(
      config: IndexConfig,
      objSources: Seq[Source[MusitObject, NotUsed]],
      sampleSource: Source[SampleObject, NotUsed]
  ) = {
    val musitObjectFlow  = new MusitObjectTypeFlow().flow(config)
    val sampleObjectFlow = new SampleTypeFlow(actorService).flow(config)

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val mergeToEs =
        builder.add(Merge[BulkCompatibleDefinition](objSources.size + 1))

      objSources.map(_.via(musitObjectFlow)).foreach(_ ~> mergeToEs)
      sampleSource.via(sampleObjectFlow) ~> mergeToEs

      SourceShape.of(mergeToEs.out)
    })
  }
}
