package repositories.elasticsearch.dao

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import repositories.analysis.dao.AnalysisDao
import utils.testdata.AnalysisGenerators

class ElasticsearchEventDaoSpec
    extends MusitSpecWithAppPerSuite
    with AnalysisGenerators
    with MusitResultValues {
  "ElasticsearchEventDao" should {

    val esEventDao   = fromInstanceCache[ElasticsearchEventDao]
    val analysisDao  = fromInstanceCache[AnalysisDao]
    implicit val mat = fromInstanceCache[Materializer]

    "publish all events" in {
      val gr = Some(dummyGenericResult())
      val e1 = dummyAnalysis(Some(oid1))
      val e2 = dummyAnalysis(Some(oid2))
      val ac = dummyAnalysisCollection(gr, e1, e2)
      analysisDao.insertCol(defaultMid, ac).futureValue.successValue

      val pub = esEventDao.analysisEvents()

      val res = Source.fromPublisher(pub).runWith(Sink.seq).futureValue

      res must have size 3
    }
  }
}
