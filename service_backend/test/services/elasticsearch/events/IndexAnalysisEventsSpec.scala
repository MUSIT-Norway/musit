package services.elasticsearch.events

import akka.stream.Materializer
import no.uio.musit.models.MuseumId
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.scalatest.Inside
import repositories.core.dao.IndexStatusDao
import services.analysis.AnalysisService
import services.elasticsearch.IndexName
import utils.testdata.{AnalysisGenerators, BaseDummyData}

import scala.concurrent.ExecutionContext

class IndexAnalysisEventsSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with AnalysisGenerators
    with BaseDummyData {

  val esIndexer       = fromInstanceCache[IndexAnalysisEvents]
  val indexStatusDao  = fromInstanceCache[IndexStatusDao]
  val analysisService = fromInstanceCache[AnalysisService]
  implicit val mat    = fromInstanceCache[Materializer]
  implicit val ec     = fromInstanceCache[ExecutionContext]

  "IndexAnalysisEvents" must {
    val au: AuthenticatedUser = dummyUser
    "index all events to elasticsearch" taggedAs ElasticsearchContainer in {
      val collection = dummySaveAnalysisCollectionCmd()
      analysisService.add(MuseumId(99), collection)(au).futureValue

      val futureIndex = esIndexer.reindexToNewIndex().futureValue
      val mbyStatus   = indexStatusDao.findLastIndexed("events").futureValue.successValue

      futureIndex mustBe a[IndexName]
      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe None
      }
    }

    "index new analysis events to elasticsearch" taggedAs ElasticsearchContainer in {
      val index = esIndexer.reindexToNewIndex().futureValue

      val collection = dummySaveAnalysisCollectionCmd()
      val blee =
        analysisService.add(MuseumId(99), collection)(au).futureValue.successValue

      println(blee.get.partOf)

      esIndexer.updateExistingIndex(index).futureValue
      val mbyStatus = indexStatusDao.findLastIndexed("events").futureValue.successValue

      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe a[Some[_]]
          status.indexed.isAfter(status.updated.value) mustBe false
      }
    }
  }

}
