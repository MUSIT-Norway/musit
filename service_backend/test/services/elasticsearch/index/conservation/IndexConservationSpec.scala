package services.elasticsearch.index.conservation

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.sksamuel.elastic4s.http.HttpClient
import models.conservation.events.{ActorRoleDate, MaterialDetermination, MaterialInfo}
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.scalatest.Inside
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import repositories.core.dao.IndexStatusDao
import services.conservation.MaterialDeterminationService
import services.elasticsearch.DocumentCount.count
import services.elasticsearch.index.conservation
import utils.testdata.{BaseDummyData, ConservationprocessGenerators}

import scala.concurrent.{ExecutionContext, Promise}
import no.uio.musit.time.dateTimeNow
/*

 containerTests:test

 If you want to run this test-only, you need to comment out the appropriate ElasticsearchContainer tags
 test-only services.elasticsearch.index.conservation.IndexConservationSpec


 */

///TODO: Remove comments around  "taggedAs ElasticsearchContainer"

class IndexConservationSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with ConservationprocessGenerators
    with BaseDummyData
    with Eventually {

  val esClient       = fromInstanceCache[HttpClient]
  val esIndexer      = fromInstanceCache[IndexConservation]
  val indexStatusDao = fromInstanceCache[IndexStatusDao]
  val matDetService  = fromInstanceCache[MaterialDeterminationService]
  implicit val as    = fromInstanceCache[ActorSystem]
  implicit val mat   = fromInstanceCache[Materializer]
  implicit val ec    = fromInstanceCache[ExecutionContext]

  "IndexConservationEvents" must {
    val timeout               = Timeout(Span(60, Seconds))
    val au: AuthenticatedUser = dummyUser
    "index conservation events to elasticsearch" taggedAs ElasticsearchContainer in {

      val matDet = dummyMaterialDetermination()
      val newMatDet =
        matDetService.add(MuseumId(99), matDet).value.futureValue.successValue
      newMatDet.isDefined mustBe true

      val p = Promise[Option[IndexConfig]]()
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => p.success(Some(in)),
          _ => p.success(None)
        )
      )
      val futureIndex = p.future.futureValue(timeout)
      futureIndex.value mustBe a[IndexConfig]

      val mbyStatus =
        indexStatusDao.findLastIndexed(conservation.indexAlias).futureValue.successValue
      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe None
      }

      eventually {
        val docCount =
          esClient.execute(count(conservation.indexAlias, conservation.conservationType))

        docCount.futureValue.count mustBe 2
      }
    }

    "index new conservation events to elasticsearch" taggedAs ElasticsearchContainer in {
      val promiseIndex = Promise[Option[IndexConfig]]()
      val futureIndex  = promiseIndex.future
      esIndexer.reindexToNewIndex(
        IndexCallback(
          in => promiseIndex.success(Some(in)),
          _ => promiseIndex.success(None)
        )
      )
      val index = futureIndex.futureValue(timeout).value

      val matDet = dummyMaterialDetermination()
      val newMatDet =
        matDetService.add(MuseumId(99), matDet).value.futureValue.successValue
      newMatDet.isDefined mustBe true

      val promiseUpdate = Promise[Option[IndexConfig]]()
      esIndexer.updateExistingIndex(
        index,
        IndexCallback(
          in => promiseUpdate.success(Some(in)),
          _ => promiseUpdate.success(None)
        )
      )
      promiseUpdate.future.futureValue(timeout)
      val mbyStatus =
        indexStatusDao.findLastIndexed(conservation.indexAlias).futureValue.successValue

      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe a[Some[_]]
          status.indexed.isAfter(status.updated.value) mustBe false
      }
    }
  }

}
