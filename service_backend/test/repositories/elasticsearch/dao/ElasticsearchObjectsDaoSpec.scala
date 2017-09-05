package repositories.elasticsearch.dao

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchObjectsDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: ElasticsearchObjectsDao  = fromInstanceCache[ElasticsearchObjectsDao]
  implicit val mat: Materializer    = fromInstanceCache[Materializer]
  implicit val ec: ExecutionContext = fromInstanceCache[ExecutionContext]

  "ElasticsearchObjectsDao" when {

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

    "museumId and collection" should {

      "return empty set when no related object are found" in {
        val res = dao.findObjectsMidAndCollection(Set(ObjectUUID.generate())).futureValue

        res mustBe empty
      }

      "find entries for object uuid" in {
        val objIdOne = ObjectUUID.fromString("89f36f77-2c27-4d33-81b4-4d4f9688950d").value
        val objIdTwo = ObjectUUID.fromString("42b6a92e-de59-4fde-9c46-5c8794be0b34").value

        val res = dao.findObjectsMidAndCollection(Set(objIdOne, objIdTwo)).futureValue

        res.head._3.uuid
        res must contain only (
          (objIdOne, MuseumId(99), Collection.fromInt(1)),
          (objIdTwo, MuseumId(99), Collection.fromInt(4))
        )
      }

    }

    "database id ranges" should {
      "include all ids in range when count is 1" in {
        val res = ElasticsearchObjectsDao.indexRanges(1, 100)

        res must have size 1
        res must contain((0, 100))
      }

      "include all ids in range when count is 2" in {
        val res = ElasticsearchObjectsDao.indexRanges(2, 100)

        res must have size 2
        res must contain((0, 50))
        res must contain((51, 100))
      }

      "include all ids in range when count is 2 and end with odd value" in {
        val res = ElasticsearchObjectsDao.indexRanges(2, 101)

        res must have size 2
        res must contain((0, 50))
        res must contain((51, 101))

      }

      "include all ids in range when count is 3 and end with odd value" in {
        val res = ElasticsearchObjectsDao.indexRanges(3, 101)

        res must have size 3
        res must contain((0, 33))
        res must contain((34, 67))
        res must contain((68, 101))
      }

    }

  }

}
