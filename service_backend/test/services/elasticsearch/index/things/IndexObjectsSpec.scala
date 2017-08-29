package services.elasticsearch.index.things

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.sksamuel.elastic4s.http.HttpClient
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import services.elasticsearch.DocumentCount._
import services.elasticsearch.index.things

import scala.concurrent.{ExecutionContext, Promise}

class IndexObjectsSpec extends MusitSpecWithAppPerSuite with Eventually {

  val esClient     = fromInstanceCache[HttpClient]
  val esIndexer    = fromInstanceCache[IndexObjects]
  implicit val as  = fromInstanceCache[ActorSystem]
  implicit val mat = fromInstanceCache[Materializer]
  implicit val ec  = fromInstanceCache[ExecutionContext]

  "IndexThings" should {
    val timeout = Timeout(Span(60, Seconds))

    "index all object to elasticsearch" taggedAs ElasticsearchContainer in {
      val p = Promise[Option[IndexConfig]]()
      val f = p.future
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => p.success(Some(in)),
          () => p.success(None)
        )
      )

      f.futureValue(timeout) mustBe defined

      eventually(timeout) {
        val samples = esClient.execute(count(things.indexAlias, things.sampleType))
        val objects = esClient.execute(count(things.indexAlias, things.objectType))

        samples.futureValue.count mustBe 1
        objects.futureValue.count mustBe 59
      }
    }
  }

}
