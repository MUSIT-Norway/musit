package repositories.elasticsearch.dao

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.test.MusitSpecWithAppPerSuite

class ElasticsearchThingsDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: ElasticsearchThingsDao = fromInstanceCache[ElasticsearchThingsDao]
  implicit val mat: Materializer  = fromInstanceCache[Materializer]

  "ElasticsearchThingsDao" should {
    "publish events" in {
      val pub = dao.objectStream

      val res = Source.fromPublisher(pub).runWith(Sink.seq).futureValue

      res.size must be >= 50
    }
  }

}
