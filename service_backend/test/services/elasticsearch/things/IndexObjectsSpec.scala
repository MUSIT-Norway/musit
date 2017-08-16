package services.elasticsearch.things

import akka.actor.ActorSystem
import akka.stream.Materializer
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import services.elasticsearch.{IndexCallback, IndexName}

import scala.concurrent.{ExecutionContext, Promise}

class IndexObjectsSpec extends MusitSpecWithAppPerSuite {

  val esIndexer    = fromInstanceCache[IndexObjects]
  implicit val as  = fromInstanceCache[ActorSystem]
  implicit val mat = fromInstanceCache[Materializer]
  implicit val ec  = fromInstanceCache[ExecutionContext]

  "IndexThings" should {
    "index all object to elasticsearch" taggedAs ElasticsearchContainer in {
      val p = Promise[Option[IndexName]]()
      val f = p.future
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => p.success(Some(in)),
          () => p.success(None)
        )
      )

      f.futureValue mustBe defined
    }
  }
}
