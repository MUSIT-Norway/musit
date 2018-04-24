package services.elasticsearch.index.conservation

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.http.search.SearchResponse
import models.conservation.events.{
  ActorRoleDate,
  ConservationEvent,
  MaterialDetermination,
  MaterialInfo
}
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.models.MuseumCollections.{Archeology, Ethnography}
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.scalatest.{BeforeAndAfterAll, Inside}
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
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import repositories.conservation.dao.ConservationDao
import services.elasticsearch.elastic4s.MusitESResponse
import services.elasticsearch.search.ConservationSearchService
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
    with BeforeAndAfterAll
    with Eventually {

  val esClient       = fromInstanceCache[HttpClient]
  val esIndexer      = fromInstanceCache[IndexConservation]
  val indexStatusDao = fromInstanceCache[IndexStatusDao]
  val matDetService  = fromInstanceCache[MaterialDeterminationService]

  val dao = fromInstanceCache[ConservationDao]

  val searchService = fromInstanceCache[ConservationSearchService]

  implicit val as  = fromInstanceCache[ActorSystem]
  implicit val mat = fromInstanceCache[Materializer]
  implicit val ec  = fromInstanceCache[ExecutionContext]

  val indexName = conservation.indexAlias

  def conservationIndexExists() = {
    val res = esClient.execute(indexExists(indexName)).await
    res.isExists
  }

  val museumId                          = MuseumId(99)
  var archeologyObjectCountAtStart      = 0
  val expectedArcheologyDocCountAtStart = 0
  val expectedDocCountAtStart           = 1

  var matDetToDelete: Option[ConservationEvent] = None

  /*Number of documents expected in the ES conservation index before we start adding local conservation events */

  def expectedDocCountBeforeTestInserts() =
    indexStatusDao
      .findLastIndexed(conservation.indexAlias)
      .futureValue
      .successValue match {
      case Some(x) => expectedDocCountAtStart
      case None    => 0
    }

  def eventuallyCountsAre(
      text: String,
      assumedNewArchCount: Integer,
      assumedNewDocCount: Integer
  ) = {
    //Useful when debugging: println("testing archDocCount: " + text)
    val assumedArchCount = expectedArcheologyDocCountAtStart + assumedNewArchCount
    eventually {

      val archDocCount = countArcheologyDocs()

      /*Useful when debugging
      if (archDocCount != assumedArchCount) {
        println(
          "(Search)ArchDocCount, Expected: " + assumedArchCount + " actually was: " + archDocCount
        )

      }  */

      archDocCount mustBe assumedArchCount
    }

    //Useful when debugging: println("testing docCount: " + text)
    val assumedTotalDocCount = expectedDocCountBeforeTestInserts() + assumedNewDocCount

    eventually {

      val docCount = esClient
        .execute(
          count(indexName, conservation.conservationType)
        )
        .futureValue
        .count

      /*Useful when debugging
      if (docCount != assumedTotalDocCount) {
        println(
          "DocCount, Expected: " + assumedTotalDocCount + " actually was: " + docCount
        )
      } */
      docCount mustBe assumedTotalDocCount
    }
  }

  override def beforeAll(): Unit = {
    if (conservationIndexExists()) {
      println("Deleted old index");
      esClient.execute(deleteIndex(indexName)).futureValue.acknowledged mustBe true
    }
    eventuallyCountsAre("beforeAll", 0, 0)

  }

  override def afterAll(): Unit = {
    if (conservationIndexExists()) {
      esClient.execute(deleteIndex(indexName)).futureValue.acknowledged mustBe true
    }
  }

  def doSearchForAll(): SearchResponse = {

    searchService
      .restrictedConservationSearch(
        mid = museumId,
        collectionIds = Seq(MuseumCollection(Archeology.uuid, None, Seq())),
        from = 0,
        limit = 1000,
        queryStr = None
      )
      .futureValue
      .successValue
      .response

  }

  def countArcheologyDocs() = {
    if (conservationIndexExists)
      doSearchForAll().hits.total
    else
      0
  }

  val localTimeout          = Timeout(Span(60, Seconds))
  val au: AuthenticatedUser = dummyUser

  def recreateIndex() = {
    val promiseIndex = Promise[Option[IndexConfig]]()
    val futureIndex  = promiseIndex.future
    esIndexer.reindexToNewIndex(
      IndexCallback(
        in => promiseIndex.success(Some(in)),
        _ => promiseIndex.success(None)
      )
    )
    futureIndex.futureValue(localTimeout).value
  }

  def updateExistingIndex(existingIndex: IndexConfig) = {
    val promiseUpdate = Promise[Option[IndexConfig]]()
    esIndexer.updateExistingIndex(
      existingIndex,
      IndexCallback(
        in => promiseUpdate.success(Some(in)),
        _ => promiseUpdate.success(None)
      )
    )
    promiseUpdate.future.futureValue(localTimeout)
  }

  "IndexConservationEvents" must {
    "check number of ES docs at the beginning" taggedAs ElasticsearchContainer in {
      eventuallyCountsAre("på starten", 0, 0)

    }
    "index conservation events to elasticsearch" taggedAs ElasticsearchContainer in {

      archeologyObjectCountAtStart = if (conservationIndexExists()) {
        eventually {
          val res = doSearchForAll()
          println("res.hits.total (in ES 2): " + res.hits.total)
          res.hits.total mustBe expectedArcheologyDocCountAtStart
          res.hits.total
        }
      } else 0

      val matDet = dummyMaterialDeterminationWithNote("---dummy1---")
      val newMatDet =
        matDetService.add(museumId, matDet).value.futureValue.successValue
      newMatDet.isDefined mustBe true
      newMatDet.get.id.isDefined mustBe true

      val index = this.recreateIndex() // promiseIndex // p.future.futureValue(localTimeout)
      index mustBe a[IndexConfig]

      val optStatus =
        indexStatusDao.findLastIndexed(conservation.indexAlias).futureValue.successValue
      inside(optStatus) {
        case Some(status) =>
          status.updated mustBe None
      }

    }
    "count result of indexing new conservation event" taggedAs ElasticsearchContainer in {

      eventuallyCountsAre("first counting", 1, 1)
    }

    "index new conservation events to elasticsearch (update index)" taggedAs ElasticsearchContainer in {

      val index = recreateIndex() // futureIndex.futureValue(localTimeout).value

      val matDet = dummyMaterialDeterminationWithNote("---Dummy2---")
      val newMatDet =
        matDetService.add(museumId, matDet).value.futureValue.successValue
      newMatDet.isDefined mustBe true
      matDetToDelete = newMatDet

      updateExistingIndex(index)
      val mbyStatus =
        indexStatusDao.findLastIndexed(conservation.indexAlias).futureValue.successValue

      inside(mbyStatus) {
        case Some(status) =>
          status.updated mustBe a[Some[_]]
          status.indexed.isAfter(status.updated.value) mustBe false
      }
    }

    "make sure new object also are indexed" taggedAs ElasticsearchContainer in {

      eventuallyCountsAre(
        "make sure new object also are indexed",
        2,
        2
      )
    }

    "deleting a conservation event also removes it from the ES-index" taggedAs ElasticsearchContainer in {

      val index = recreateIndex() // futureIndex.futureValue(localTimeout).value

      eventuallyCountsAre(
        "make sure new object also are indexed (before deletion)",
        2,
        2
      )
      val matDet      = matDetToDelete.get
      val idsToDelete = Seq(matDet.id.get)

      dao.updateCpAndDeleteSubEvent(museumId, matDet.id.get)

      updateExistingIndex(index)

      eventuallyCountsAre(
        "make sure new object also are indexed (after deletion). but the removed one is removed.",
        1, //Here we should not get the deleted objects
        2 //Includes all objects in the index, both deleted and existing ones
      )

    }
  }
}
