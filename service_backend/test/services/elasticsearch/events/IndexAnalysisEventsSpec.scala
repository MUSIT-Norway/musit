package services.elasticsearch.events

import akka.stream.Materializer
import no.uio.musit.models.MuseumId
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import services.analysis.AnalysisService
import services.elasticsearch.IndexName
import utils.testdata.{AnalysisGenerators, BaseDummyData}

import scala.concurrent.ExecutionContext

class IndexAnalysisEventsSpec
    extends MusitSpecWithAppPerSuite
    with AnalysisGenerators
    with BaseDummyData {

  val esIndexer       = fromInstanceCache[IndexAnalysisEvents]
  val analysisService = fromInstanceCache[AnalysisService]
  implicit val mat    = fromInstanceCache[Materializer]
  implicit val ec     = fromInstanceCache[ExecutionContext]

  "IndexAnalysisEvents" must {
    val au: AuthenticatedUser = dummyUser
    "index all events to elasticsearch" taggedAs ElasticsearchContainer in {
      val collection = dummySaveAnalysisCollectionCmd()
      analysisService.add(MuseumId(99), collection)(au).futureValue

      val f = esIndexer.reindexToNewIndex()

      f.futureValue mustBe a[IndexName]
    }
  }

}
