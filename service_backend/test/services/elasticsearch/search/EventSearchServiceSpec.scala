package services.elasticsearch.search

import com.sksamuel.elastic4s.DocumentRef
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, deleteIndex, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import models.elasticsearch._
import com.sksamuel.elastic4s.playjson._
import no.uio.musit.models.MuseumCollections.{Collection, Ethnography, Moss}
import no.uio.musit.models.{EventId, EventTypeId, MuseumCollection, MuseumId}
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import services.elasticsearch.index.events.EventIndexConfig
import services.elasticsearch.index.{IndexMaintainer, events}
import utils.testdata.BaseDummyData

import scala.concurrent.ExecutionContext
import scala.util.Random

class EventSearchServiceSpec
    extends MusitSpecWithAppPerSuite
    with Eventually
    with BeforeAndAfterAll
    with BaseDummyData
    with MusitResultValues {

  private[this] val client          = fromInstanceCache[HttpClient]
  private[this] val indexMaintainer = fromInstanceCache[IndexMaintainer]
  private[this] val service         = fromInstanceCache[EventSearchService]

  private[this] implicit val ec = fromInstanceCache[ExecutionContext]

  private[this] val indexName = events.indexAlias + Random.nextInt(Int.MaxValue)

  val evtId_10 = EventId(10)
  val evtId_11 = EventId(11)
  val evtId_12 = EventId(12)

  val evtId_20 = EventId(20)
  val evtId_21 = EventId(21)
  val evtId_22 = EventId(22)

  val evtId_30 = EventId(30)
  val evtId_31 = EventId(31)
  val evtId_32 = EventId(32)

  val museum1 = MuseumId(1)
  val museum2 = MuseumId(2)

  val collection1 = Ethnography
  val collection2 = Moss

  "EventSearchService" should {

    "only return documents to the with the right museums id and collection" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedEventsSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = None
        )
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toEventId) must contain only (evtId_10, evtId_11, evtId_12)
    }

    "search should return a subset" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedEventsSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = Some(""" id:11 AND _type:"analysis" """)
        )
        .futureValue
        .successValue
        .response

      // id 10 comes from that we're searching in paren/child for the query as well.
      // This isn't wrong but we should probably adjust the score/boost.
      res.hits.hits.map(toEventId) must contain only (evtId_11, evtId_10)
    }

    "not by pass the authorization in `q`" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedEventsSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = Some(s"collection.uuid: ${collection2.uuid}")
        )
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toEventId) must contain noneOf (
        evtId_20, evtId_21, evtId_22,
        evtId_30, evtId_31, evtId_32
      )
    }

    "analysisCollection should include `inner-hits` from `analysis`" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedEventsSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = None
        )
        .futureValue
        .successValue
        .response

      val innerHits =
        findInnerHit(res, evtId_11, EventSearchService.analysisCollectionInnerHitName)

      innerHits must not be empty
    }

    "analysis should include `inner-hits` from `analysis collection`" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedEventsSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = None
        )
        .futureValue
        .successValue
        .response

      val innerHits =
        findInnerHit(res, evtId_10, EventSearchService.analyseInnerHitName)

      innerHits must not be empty
    }
  }

  private def findInnerHit(res: SearchResponse, id: EventId, innerHitKey: String) = {
    res.hits.hits
      .find(toEventId(_) == id)
      .map(_.innerHits)
      .getOrElse(Map.empty)
      .get(innerHitKey)
      .map(_.hits)
  }

  private def toEventId(s: SearchHit) = EventId(s.id.toLong)

  override def beforeAll(): Unit = {
    val setup = for {
      m <- client.execute(EventIndexConfig.config(indexName))
      if m.acknowledged
      res <- client.execute(
              bulk(
                // case 1
                indexAnalysisCollectionDoc(evtId_10),
                indexAnalysisDoc(evtId_11, evtId_10, museum1, collection1),
                indexSampleCreatedDoc(evtId_12, museum1, collection1),
                // case 2
                indexAnalysisCollectionDoc(evtId_20),
                indexAnalysisDoc(evtId_21, evtId_20, museum2, collection2),
                indexSampleCreatedDoc(evtId_22, museum2, collection2),
                // case 3
                indexAnalysisCollectionDoc(evtId_30),
                indexAnalysisDoc(evtId_31, evtId_30, museum1, collection2),
                indexSampleCreatedDoc(evtId_32, museum1, collection2)
              ).refresh(RefreshPolicy.IMMEDIATE)
            )
      _ <- indexMaintainer.activateIndex(indexName, events.indexAlias)
    } yield res

    setup.futureValue
  }

  override def afterAll(): Unit = {
    client.execute(deleteIndex(indexName)).futureValue
  }

  private def indexAnalysisCollectionDoc(id: EventId) = {
    val d = AnalysisCollectionSearch(
      id = id,
      analysisTypeId = EventTypeId(1),
      doneBy = None,
      registeredBy = None,
      responsible = None,
      administrator = None,
      updatedBy = None,
      completedBy = None,
      note = Some("analysis collection note"),
      extraAttributes = None,
      result = None,
      restriction = None,
      reason = None,
      status = None,
      caseNumbers = None,
      orgId = None
    )
    val dId = id.underlying.toString
    indexInto(indexName, events.analysisCollectionType) id dId doc d
  }

  private def indexAnalysisDoc(
      id: EventId,
      partOf: EventId,
      mid: MuseumId,
      col: Collection
  ) = {
    val d = AnalysisSearch(
      id = id,
      museumId = Some(mid),
      collection = Some(CollectionSearch(col)),
      analysisTypeId = EventTypeId(1),
      doneBy = None,
      registeredBy = None,
      responsible = None,
      administrator = None,
      updatedBy = None,
      completedBy = None,
      objectId = None,
      objectType = None,
      partOf = Some(partOf),
      note = Some("analysis note"),
      extraAttributes = None,
      result = None
    )

    val dId = id.underlying.toString
    val pId = partOf.underlying.toString
    indexInto(indexName, events.analysisType) id dId doc d parent pId
  }

  private def indexSampleCreatedDoc(id: EventId, mid: MuseumId, col: Collection) = {
    val d = SampleCreatedSearch(
      id = id,
      museumId = Some(mid),
      collection = Some(CollectionSearch(col)),
      doneBy = None,
      registeredBy = None,
      objectId = None,
      sampleObjectId = None,
      externalLinks = None
    )
    val dId = id.underlying.toString
    indexInto(indexName, events.sampleType) id dId doc d
  }

}
