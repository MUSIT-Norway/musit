package services.elasticsearch.things

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch.{MusitObjectSearch, MustObjectSearch}
import models.musitobject.MusitObject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.Configuration
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchThingsDao
import services.elasticsearch.{ElasticsearchFlow, IndexMaintainer, IndexName, Indexer}

import scala.concurrent.{ExecutionContext, Future}

class IndexMusitObjects @Inject()(
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
    cfg
      .getInt("musit.elasticsearch.streams.musitObjects.concurrentSources")
      .getOrElse(20)
  private[this] val fetchSize =
    cfg
      .getInt("musit.elasticsearch.streams.musitObjects.dbStreamFetchSize")
      .getOrElse(1000)

  override val indexAliasName = "musit_objects"

  override val elasticsearchFlow = new ElasticsearchFlow(client, esBathSize)

  val populate =
    Flow[MusitObject].filter(_.uuid.isDefined).map(mObj => MustObjectSearch(mObj))

  override def toAction(indexName: IndexName) =
    Flow[MusitObjectSearch].map { thing =>
      indexInto(indexName.name, thing.documentType) id thing.documentId doc thing
    }

  override def reindexToNewIndex(): Future[IndexName] = {
    val indexName = createIndexName()
    for {
      _ <- client.execute(MusitObjectsIndexConfig.config(indexName.name))
      _ <- reindexMusitObjects(indexName)
    } yield indexName
  }

  override def updateExistingIndex(indexName: IndexName): Future[Unit] =
    for {
      optIndexAfter <- findLastIndexDateTime()
      _ <- optIndexAfter.map { indexAfter =>
            updateIndexMusitObjects(indexName, indexAfter)
          }.getOrElse(Future.successful(Done))
    } yield ()

  private def findLastIndexDateTime(): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case err: MusitError => None
    }
  }

  private def updateIndexMusitObjects(
      indexName: IndexName,
      fetchObjectsAfter: DateTime
  ): Future[Done] = {
    val pub = elasticsearchThingsDao
      .objectsChangedAfterTimstampStream(fetchSize, fetchObjectsAfter)

    index(Seq(Source.fromPublisher(pub).via(populate)))
    val now = time.dateTimeNow
    elasticsearchThingsDao
      .objectStreams(concurrentSources, fetchSize)
      .map(s => s.map(Source.fromPublisher))
      .flatMap { sources =>
        reindex(
          sources.map(_.via(populate)),
          Some(indexName),
          (_, alias) => indexStatusDao.update(alias, now).map(_ => ())
        )
      }
  }

  private def reindexMusitObjects(
      indexName: IndexName
  ): Future[Done] = {
    val indexStarting = time.dateTimeNow
    elasticsearchThingsDao
      .objectStreams(concurrentSources, fetchSize)
      .map(s => s.map(Source.fromPublisher))
      .flatMap { sources =>
        reindex(
          sources.map(_.via(populate)),
          Some(indexName),
          (_, alias) => indexStatusDao.indexed(alias, indexStarting).map(_ => ())
        )
      }
  }

}
