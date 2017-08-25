package services.elasticsearch

import java.util.UUID

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import models.analysis.LeftoverSamples.NoLeftover
import models.analysis.{ParentObject, SampleTypeId}
import models.analysis.SampleStatuses.Intact
import models.elasticsearch.{CollectionSearch, MusitObjectSearch, SampleObjectSearch}
import no.uio.musit.models.MuseumCollections.{Archeology, Collection, Ethnography}
import no.uio.musit.models.ObjectTypes.CollectionObjectType
import no.uio.musit.models._
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import services.elasticsearch.things.MusitObjectsIndexConfig
import utils.testdata.BaseDummyData

import scala.concurrent.ExecutionContext
import scala.util.Random

class SearchServiceSpec
    extends MusitSpecWithAppPerSuite
    with Eventually
    with BeforeAndAfterAll
    with BaseDummyData {

  private[this] val client          = fromInstanceCache[HttpClient]
  private[this] val indexMaintainer = fromInstanceCache[IndexMaintainer]
  private[this] val service         = fromInstanceCache[SearchService]

  private[this] implicit val ec = fromInstanceCache[ExecutionContext]

  private[this] val indexName = things.indexAlias + Random.nextInt(Int.MaxValue)

  val obj1inCol1 = ObjectUUID.fromString("b0d8ff68-9c5c-4da5-9ef3-a0000000a001").value
  val obj2inCol2 = ObjectUUID.fromString("11b3f5ab-dff6-4b80-90bd-a0000000a002").value
  val obj3inCol2 = ObjectUUID.fromString("74ac428d-8842-469e-8368-a0000000a003").value
  val obj4inCol2 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a004").value
  val obj5inCol1 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a005").value

  val sam1FromObj1inCol1 =
    ObjectUUID.fromString("145164cb-1699-4c15-aab5-b0000000b001").value
  val sam2FromObj2inCol2 =
    ObjectUUID.fromString("9ae58109-8b44-402b-a7b9-b0000000b002").value

  "SearchService" should {

    "only return documents to the with the right museums id and collection" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(1),
          collectionIds = Seq(MuseumCollection(Archeology.uuid, None, Seq())),
          museumNo = None,
          subNo = None,
          term = None,
          q = None
        )(dummyUser)
        .futureValue

      res.hits.hits.map(toObjectUUID) must contain only (obj1inCol1, sam1FromObj1inCol1)
    }

    "search on MuseumNo with the result of one object and one sample " taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          museumNo = Some("C402"),
          subNo = None,
          term = None,
          q = None
        )(dummyUser)
        .futureValue

      res.hits.hits.map(toObjectUUID) must contain only (obj2inCol2, sam2FromObj2inCol2)
    }

    "search on MuseumNo and subNo with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          museumNo = Some("C1610"),
          subNo = Some("b"),
          term = None,
          q = None
        )(dummyUser)
        .futureValue

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }

    "search where q has no restrictions" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          museumNo = None,
          subNo = None,
          term = None,
          q = Some("C402")
        )(dummyUser)
        .futureValue

      res.hits.hits.map(toObjectUUID) must contain only (obj2inCol2, sam2FromObj2inCol2)
    }

    "search where q has restrictions on subNo" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          museumNo = None,
          subNo = None,
          term = None,
          q = Some("C1610 AND subNo: b")
        )(dummyUser)
        .futureValue

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }

  }

  def toObjectUUID(s: SearchHit) = ObjectUUID(UUID.fromString(s.id))

  override def beforeAll(): Unit = {
    val setup = for {
      m <- client.execute(MusitObjectsIndexConfig.config(indexName))
      if m.acknowledged
      res <- client.execute(
              bulk(
                // Museum 1
                // obj with sample
                indexMusitObjectDoc(
                  obj1inCol1,
                  MuseumId(1),
                  Archeology,
                  MuseumNo("C1378")
                ),
                indexSampleDoc(obj1inCol1, sam1FromObj1inCol1, MuseumId(1)),
                // obj
                indexMusitObjectDoc(
                  obj5inCol1,
                  MuseumId(1),
                  Ethnography,
                  MuseumNo("C1378")
                ),
                // Museum 2
                // obj with sample
                indexMusitObjectDoc(
                  obj2inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("C402")
                ),
                indexSampleDoc(obj2inCol2, sam2FromObj2inCol2, MuseumId(2)),
                // obj
                indexMusitObjectDoc(
                  obj3inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("C1610"),
                  Some(SubNo("b"))
                ),
                //obj
                indexMusitObjectDoc(
                  obj4inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("C1610"),
                  Some(SubNo("c"))
                )
              ).refresh(RefreshPolicy.IMMEDIATE)
            )
      _ <- indexMaintainer.activateIndex(indexName, things.indexAlias)
    } yield res

    setup.futureValue
  }

  override def afterAll(): Unit = {
    client.execute(deleteIndex(indexName)).futureValue
  }

  private def indexMusitObjectDoc(
      id: ObjectUUID,
      museumId: MuseumId,
      collection: Collection,
      museumNo: MuseumNo,
      subNo: Option[SubNo] = None,
      term: String = "maske"
  ) = {
    val d = MusitObjectSearch(
      id,
      museumId,
      museumNo,
      subNo,
      term,
      None,
      Some(CollectionSearch(collection.id, collection.uuid.underlying)),
      None,
      None,
      None,
      None,
      None
    )
    indexInto(indexName, things.objectType) id id.underlying.toString doc d
  }

  private def indexSampleDoc(
      fromObject: ObjectUUID,
      sampleId: ObjectUUID,
      museumId: MuseumId
  ) = {
    val d = SampleObjectSearch(
      Some(sampleId),
      fromObject,
      ParentObject(Some(fromObject), CollectionObjectType),
      isExtracted = false,
      museumId,
      Intact,
      None,
      None,
      None,
      None,
      None,
      SampleTypeId(1),
      None,
      None,
      None,
      None,
      NoLeftover,
      None,
      None,
      None,
      None
    )

    val dId = sampleId.underlying.toString
    val pId = fromObject.underlying.toString
    indexInto(indexName, things.sampleType) id dId doc d parent pId
  }
}
