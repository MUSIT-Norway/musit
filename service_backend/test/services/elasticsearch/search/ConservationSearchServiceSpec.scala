package services.elasticsearch.search

import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, deleteIndex, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import com.sksamuel.elastic4s.playjson._
import models.conservation.events.{ActorRoleDate, MaterialDetermination, MaterialInfo}
import models.elasticsearch._
import no.uio.musit.models.MuseumCollections.{Collection, Ethnography, Moss}
import no.uio.musit.models._
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import no.uio.musit.time.dateTimeNow
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import services.elasticsearch.index.conservation.ConservationIndexConfig
import services.elasticsearch.index.{IndexMaintainer, conservation}
import utils.testdata.BaseDummyData

import scala.concurrent.ExecutionContext
import scala.util.Random

/*

 containerTests:test

 If you want to run this test-only, you need to comment out the appropriate ElasticsearchContainer tags
 test-only services.elasticsearch.search.ConservationSearchServiceSpec


 */

class ConservationSearchServiceSpec
    extends MusitSpecWithAppPerSuite
    with Eventually
    with BeforeAndAfterAll
    with BaseDummyData
    with MusitResultValues {

  private[this] val client          = fromInstanceCache[HttpClient]
  private[this] val indexMaintainer = fromInstanceCache[IndexMaintainer]
  private[this] val service         = fromInstanceCache[ConservationSearchService]

  private[this] implicit val ec = fromInstanceCache[ExecutionContext]

  private[this] val indexName = conservation.indexAlias + Random.nextInt(Int.MaxValue)

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

  "ConservationEventSearchService" should {

    "only return documents with the right museums id and collection" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedConservationSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = None
        )
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toEventId) must contain only (evtId_11, evtId_12)
    }

    "search should return a subset" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedConservationSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = Some(""" _id:11 AND _type:"conservation" """)
        )
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toEventId) must contain only (evtId_11)
    }

    "not by pass the authorization in `q`" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedConservationSearch(
          mid = museum1,
          collectionIds = Seq(MuseumCollection(collection1.uuid, None, Seq())),
          from = 0,
          limit = 10,
          queryStr = Some(s"collectionUuid: ${collection2.uuid}")
        )
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toEventId) must contain noneOf (
        evtId_20, evtId_21, evtId_22,
        evtId_30, evtId_31, evtId_32
      )
    }
  }

  private def toEventId(s: SearchHit) = EventId(s.id.toLong)

  override def beforeAll(): Unit = {
    val setup = for {
      m <- client.execute(ConservationIndexConfig.config(indexName))
      if m.acknowledged
      res <- client.execute(
              bulk(
                // case 1
                indexConservationEventDoc(evtId_10, museum1, None),
                indexConservationEventDoc(evtId_11, museum1, Some(collection1)),
                indexConservationEventDoc(evtId_12, museum1, Some(collection1)),
                // case 2
                indexConservationEventDoc(evtId_21, museum2, Some(collection2)),
                indexConservationEventDoc(evtId_22, museum2, Some(collection2)),
                // case 3
                indexConservationEventDoc(evtId_30, museum2, Some(collection2)),
                indexConservationEventDoc(evtId_31, museum1, Some(collection2)),
                indexConservationEventDoc(evtId_32, museum1, Some(collection2))
              ).refresh(RefreshPolicy.IMMEDIATE)
            )
      _ <- indexMaintainer.activateIndex(indexName, conservation.indexAlias)
    } yield res

    setup.futureValue
  }

  override def afterAll(): Unit = {
    //client.execute(deleteIndex(indexName)).futureValue
  }

  private def indexConservationEventDoc(
      id: EventId,
      mid: MuseumId,
      optCol: Option[Collection]
  ) = {
    val event = dummyMaterialDetermination(id)
    val d = ConservationSearch(
      mid,
      optCol.map(_.uuid),
      event
    )
    val dId = id.underlying.toString
    indexInto(indexName, conservation.conservationType) id dId doc d
  }

  def dummyMaterialDetermination(
      eventId: EventId,
      oids: Option[Seq[ObjectUUID]] = Some(Seq(ObjectUUID.generate()))
  ): MaterialDetermination = {
    val now = Some(dateTimeNow)
    MaterialDetermination(
      id = None,
      eventTypeId = MaterialDetermination.eventTypeId,
      registeredBy = None,
      registeredDate = now,
      updatedBy = None,
      updatedDate = now,
      completedBy = None,
      completedDate = None,
      partOf = None,
      note = Some("hurra note"),
      actorsAndRoles = Some(
        Seq(
          ActorRoleDate(
            1,
            ActorId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"),
            now
          )
        )
      ),
      affectedThings = oids,
      documents = Some(
        Seq(FileId.unsafeFromString("d63ab290-2fab-42d2-9b57-2475dfbd0b3c"))
      ),
      materialInfo = Some(Seq(MaterialInfo(1, Some("veldig spes materiale"), Some(1))))
    )
  }

}
