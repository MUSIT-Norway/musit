package services.elasticsearch.things

import akka.Done
import akka.stream.Materializer
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}

import scala.concurrent.ExecutionContext

class IndexMusitObjectsSpec extends MusitSpecWithAppPerSuite {

  val esIndexer    = fromInstanceCache[IndexMusitObjects]
  implicit val mat = fromInstanceCache[Materializer]
  implicit val ec  = fromInstanceCache[ExecutionContext]

  "IndexThings" should {
    "index all object to elasticsearch" taggedAs ElasticsearchContainer in {
      val f = esIndexer.reindexAll()

      f.futureValue mustBe Done
    }

  }
}
