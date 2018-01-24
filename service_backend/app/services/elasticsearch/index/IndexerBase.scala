package services.elasticsearch.index

import akka.stream.Materializer
import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import org.joda.time.DateTime
import play.api.Configuration
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.ElasticsearchObjectsDao
import services.actor.ActorService

import scala.concurrent.{ExecutionContext, Future}

abstract class IndexerBase(
    indexStatusDao: IndexStatusDao
) extends Indexer {

  /*
  class IndexAnalysis @Inject()(
                                 elasticsearchEventDao: ElasticsearchEventDao,
                                 elasticsearchObjectsDao: ElasticsearchObjectsDao,
                                 indexStatusDao: IndexStatusDao,
                                 actorService: ActorService,
                                 client: HttpClient,
                                 cfg: Configuration,
                                 override val indexMaintainer: IndexMaintainer
                               ) extends Indexer {


    class IndexObjects @Inject()(
                                elasticsearchObjectsDao: ElasticsearchObjectsDao,
                                indexStatusDao: IndexStatusDao,
                                actorService: ActorService,
                                client: HttpClient,
                                cfg: Configuration,
                                override val indexMaintainer: IndexMaintainer
                              )(implicit ec: ExecutionContext, mat: Materializer)
   */

  protected def findLastIndexDateTime()(
      implicit ec: ExecutionContext
  ): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case _: MusitError   => None
    }
  }
}
