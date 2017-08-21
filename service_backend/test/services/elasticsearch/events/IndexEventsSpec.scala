package services.elasticsearch.events

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.models.MuseumId
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.scalatest.Inside
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import repositories.core.dao.IndexStatusDao
import services.analysis.AnalysisService
import services.elasticsearch.DocumentCount.count
import services.elasticsearch.{events, things}
import utils.testdata.{AnalysisGenerators, BaseDummyData}

import scala.concurrent.{ExecutionContext, Promise}

class IndexEventsSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with AnalysisGenerators
    with BaseDummyData
    with Eventually {

  val esClient        = fromInstanceCache[HttpClient]
  val esIndexer       = fromInstanceCache[IndexEvents]
  val indexStatusDao  = fromInstanceCache[IndexStatusDao]
  val analysisService = fromInstanceCache[AnalysisService]
  implicit val as     = fromInstanceCache[ActorSystem]
  implicit val mat    = fromInstanceCache[Materializer]
  implicit val ec     = fromInstanceCache[ExecutionContext]

  "IndexAnalysisEvents" must {
    val timeout               = Timeout(Span(60, Seconds))
    val au: AuthenticatedUser = dummyUser
    "index all events to elasticsearch" taggedAs ElasticsearchContainer in {
      val collection = dummySaveAnalysisCollectionCmd()
      analysisService.add(MuseumId(99), collection)(au).futureValue

      val p = Promise[Option[IndexConfig]]()
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => p.success(Some(in)),
          () => p.success(None)
        )
      )
      val futureIndex = p.future.futureValue(timeout)
      futureIndex.value mustBe a[IndexConfig]

      val mbyStatus =
        indexStatusDao.findLastIndexed(events.indexAlias).futureValue.successValue
      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe None
      }

      eventually {
        val a  = esClient.execute(count(events.indexAlias, events.analysisType))
        val ac = esClient.execute(count(events.indexAlias, events.analysisCollectionType))
        val s  = esClient.execute(count(events.indexAlias, events.sampleType))

        a.futureValue.count mustBe 1
        ac.futureValue.count mustBe 1
        s.futureValue.count mustBe 0
      }

    }

    "index new analysis events to elasticsearch" taggedAs ElasticsearchContainer in {
      val promiseIndex = Promise[Option[IndexConfig]]()
      val futureIndex  = promiseIndex.future
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => promiseIndex.success(Some(in)),
          () => promiseIndex.success(None)
        )
      )
      val index = futureIndex.futureValue(timeout).value

      val collection = dummySaveAnalysisCollectionCmd()
      analysisService.add(MuseumId(99), collection)(au).futureValue.successValue

      val promiseUpdate = Promise[Option[IndexConfig]]()
      esIndexer.updateExistingIndex(
        index,
        IndexCallback(
          in => promiseUpdate.success(Some(in)),
          () => promiseUpdate.success(None)
        )
      )
      promiseUpdate.future.futureValue(timeout)
      val mbyStatus = indexStatusDao.findLastIndexed("events").futureValue.successValue

      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe a[Some[_]]
          status.indexed.isAfter(status.updated.value) mustBe false
      }
    }
  }

}
