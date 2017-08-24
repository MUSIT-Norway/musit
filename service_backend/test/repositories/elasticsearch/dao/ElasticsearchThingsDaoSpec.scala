package repositories.elasticsearch.dao

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchThingsDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: ElasticsearchThingsDao   = fromInstanceCache[ElasticsearchThingsDao]
  implicit val mat: Materializer    = fromInstanceCache[Materializer]
  implicit val ec: ExecutionContext = fromInstanceCache[ExecutionContext]

  "ElasticsearchThingsDao" when {

    "musit object" should {
      "stream all events on objects streams" in {
        val streams = dao.objectStreams(streams = 2, fetchSize = 20)

        val res = streams.flatMap { s =>
          Future.sequence(
            s.map { _.fold(0) { case (c, obj) => c + 1 }.runWith(Sink.head) }
          )
        }.futureValue

        res.sum must be >= 50
      }
    }

    "sample object" should {
      val sampleRegisteredDateTime = DateTime.parse("2015-12-31T23:00:00.000Z")

      "stream all samples" in {
        val stream = dao.sampleStream(fetchSize = 20, None)

        val res = stream.runWith(Sink.seq).futureValue

        res.size must be >= 1
      }

      "should exclude events after a given timestamp" in {
        val stream =
          dao.sampleStream(fetchSize = 20, Some(sampleRegisteredDateTime.plusHours(1)))

        val res = stream.runWith(Sink.seq).futureValue

        res.size mustBe 0
      }

      "should include events after a given timestamp" in {
        val stream =
          dao.sampleStream(fetchSize = 20, Some(sampleRegisteredDateTime.minusHours(1)))

        val res = stream.runWith(Sink.seq).futureValue

        res.size must be >= 1
      }

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
