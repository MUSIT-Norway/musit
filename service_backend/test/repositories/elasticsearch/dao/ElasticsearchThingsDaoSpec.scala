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
          s.map { _.fold(0) { case (c, obj) => c + 1 }.runWith(Sink.head) }
        )
      }.futureValue

      res.sum must be >= 50
    }

    "database id ranges" should {
      "include all ids in range when count is 1" in {
        val res = ElasticsearchThingsDao.indexRanges(1, 100)

        res must have size 1
        res must contain((0, 100))
      }

      "include all ids in range when count is 2" in {
        val res = ElasticsearchThingsDao.indexRanges(2, 100)

        res must have size 2
        res must contain((0, 50))
        res must contain((51, 100))
      }

      "include all ids in range when count is 2 and end with odd value" in {
        val res = ElasticsearchThingsDao.indexRanges(2, 101)

        res must have size 2
        res must contain((0, 50))
        res must contain((51, 101))

      }

      "include all ids in range when count is 3 and end with odd value" in {
        val res = ElasticsearchThingsDao.indexRanges(3, 101)

        res must have size 3
        res must contain((0, 33))
        res must contain((34, 67))
        res must contain((68, 101))
      }

    }

  }

}
