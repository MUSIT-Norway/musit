package repositories.elasticsearch.dao

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class ElasticsearchThingsDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: ElasticsearchThingsDao = fromInstanceCache[ElasticsearchThingsDao]
  implicit val mat: Materializer  = fromInstanceCache[Materializer]

  "ElasticsearchThingsDao" should {
    "publish events on multiple streams" in {
      val pubs = dao.objectStreams(streams = 2, fetchSize = 20)

      val res = pubs.flatMap { s =>
        Future.sequence(
          s.map { pub =>
            Source
              .fromPublisher(pub)
              .fold(0) { case (c, obj) => c + 1 }
              .runWith(Sink.head)
          }
        )
      }.futureValue

      res.sum must be >= 50
    }

  }

}
