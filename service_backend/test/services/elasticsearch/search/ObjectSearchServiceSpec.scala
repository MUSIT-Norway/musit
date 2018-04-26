package services.elasticsearch.search

import java.util.UUID

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.playjson._
import models.analysis.LeftoverSamples.NoLeftover
import models.analysis.SampleStatuses.Intact
import models.analysis.{ParentObject, SampleTypeId}
import models.elasticsearch.{CollectionSearch, MusitObjectSearch, SampleObjectSearch}
import no.uio.musit.models.MuseumCollections.{Archeology, Collection, Ethnography}
import no.uio.musit.models.ObjectTypes.CollectionObjectType
import no.uio.musit.models._
import no.uio.musit.security.Permissions.GodMode
import no.uio.musit.security.{AccessAll, GroupInfo, ModuleConstraint}
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import services.elasticsearch.index.{IndexMaintainer, objects}
import services.elasticsearch.index.objects.MusitObjectsIndexConfig
import utils.testdata.BaseDummyData

import scala.concurrent.ExecutionContext
import scala.util.Random

class ObjectSearchServiceSpec
    extends MusitSpecWithAppPerSuite
    with Eventually
    with BeforeAndAfterAll
    with BaseDummyData
    with MusitResultValues {

  private[this] val client          = fromInstanceCache[HttpClient]
  private[this] val indexMaintainer = fromInstanceCache[IndexMaintainer]
  private[this] val service         = fromInstanceCache[ObjectSearchService]

  private[this] implicit val ec = fromInstanceCache[ExecutionContext]

  private[this] val indexName = objects.indexAlias + Random.nextInt(Int.MaxValue)

  val godUser = dummyUser.copy(
    groups = Seq(
      GroupInfo(
        GroupId(UUID.randomUUID()),
        "test",
        AccessAll,
        GodMode,
        defaultMuseumId,
        None,
        Seq()
      )
    )
  )

  val obj1inCol1 = ObjectUUID.fromString("b0d8ff68-9c5c-4da5-9ef3-a0000000a001").value
  val obj2inCol2 = ObjectUUID.fromString("11b3f5ab-dff6-4b80-90bd-a0000000a002").value
  val obj3inCol2 = ObjectUUID.fromString("74ac428d-8842-469e-8368-a0000000a003").value
  val obj4inCol2 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a004").value
  val obj5inCol1 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a005").value
  val obj6inCol2 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a006").value
  val obj7inCol2 = ObjectUUID.fromString("409d83ec-d561-4811-adb3-a0000000a007").value

  val sam1FromObj1inCol1 =
    ObjectUUID.fromString("145164cb-1699-4c15-aab5-b0000000b001").value
  val sam2FromObj2inCol2 =
    ObjectUUID.fromString("9ae58109-8b44-402b-a7b9-b0000000b002").value
  val sam3FromObj6inCol2 =
    ObjectUUID.fromString("9ae58109-8b44-402b-a7b9-b0000000b003").value

  "ObjectSearchService" should {

    "include all documents from a museum when user has god mode" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = None
        )(godUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only (
        obj2inCol2, sam2FromObj2inCol2, obj3inCol2, obj4inCol2, obj7inCol2
        //, obj6inCol2, sam3FromObj6inCol2 // deleted
      )
    }

    "only return documents to the with the right museums id and collection" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(1),
          collectionIds = Seq(MuseumCollection(Archeology.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only (obj1inCol1, sam1FromObj1inCol1)
    }

    "samples must include objects in `inner-hits`" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(1),
          collectionIds = Seq(MuseumCollection(Archeology.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      val sampleResult =
        res.hits.hits.filter(_.id == sam1FromObj1inCol1.underlying.toString).head

      val innerHits = sampleResult.innerHits
        .get(ObjectSearchService.innerHitParentName)
        .map(a => a.hits)

      innerHits must not be empty
    }

    "search must not include deleted documents " taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain noneOf (obj6inCol2, sam3FromObj6inCol2)
    }

    "search on MuseumNo with the result of one object and one sample " taggedAs ElasticsearchContainer in {
      Thread.sleep(1005)
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("c-402")),
          subNo = None,
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only (obj2inCol2, sam2FromObj2inCol2)
    }

    "search on MuseumNo and subNo with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("c1610")),
          subNo = Some(SubNo("b")),
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }

    "search where q has no restrictions" taggedAs ElasticsearchContainer in {
      //Thread.sleep(1005)
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 20,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = Some("c-402")
        )(dummyUser)
        .futureValue
        .successValue
        .response
      res.hits.hits
        .map(toObjectUUID) must contain only (obj2inCol2, sam2FromObj2inCol2, obj4inCol2)
      //search for both c and 402
    }

    "search where q has restrictions on subNo" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = Some("C1610 AND subNo: b")
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }

    "search with q and other parameters" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = Some(SubNo("b")),
          term = None,
          queryStr = Some("C1610")
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }
    "search on MuseumNo and subNo(with space) with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(1),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("c-602")),
          subNo = Some(SubNo("b d")),
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj5inCol1
    }
    "search on aggregated taxon/class with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = None,
          subNo = None,
          term = None,
          queryStr = Some("taxontull")
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }
    "search on aggregated taxon/class and museumno and subno with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("c1610")),
          subNo = Some(SubNo("b")),
          term = None,
          queryStr = Some("taxontull")
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj3inCol2
    }
    "search on term on lowercase/uppercase with the result of one object" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("C1610")),
          subNo = Some(SubNo("c")),
          term = Some("TUSENBEN"),
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits.map(toObjectUUID) must contain only obj4inCol2
    }
    "search on museumNo with the result sorted by museumNo and SubNo" taggedAs ElasticsearchContainer in {
      val res = service
        .restrictedObjectSearch(
          mid = MuseumId(2),
          collectionIds = Seq(MuseumCollection(Ethnography.uuid, None, Seq())),
          limit = 10,
          from = 0,
          museumNo = Some(MuseumNo("C1610")),
          subNo = None,
          term = None,
          queryStr = None
        )(dummyUser)
        .futureValue
        .successValue
        .response

      res.hits.hits
        .map(toObjectUUID) must contain only (obj4inCol2, obj7inCol2, obj3inCol2)
      res.hits.hits.headOption.get.id mustBe obj7inCol2.underlying.toString

    }

  }

  def toObjectUUID(s: SearchHit) = {
    println("")
    println("OBJEKTER " + s)
    ObjectUUID(UUID.fromString(s.id))

  }

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
                indexMusitObjectDoc(
                  obj5inCol1,
                  MuseumId(1),
                  Ethnography,
                  MuseumNo("c-602"),
                  Some(SubNo("b d"))
                ),
                // Museum 2
                // obj with sample
                indexMusitObjectDoc(
                  obj2inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("c-402")
                ),
                indexSampleDoc(obj2inCol2, sam2FromObj2inCol2, MuseumId(2)),
                // obj
                indexMusitObjectDoc(
                  obj3inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("c1610"),
                  Some(SubNo("b")),
                  aggregatedClassData = Some("taxontull")
                ),
                //obj
                indexMusitObjectDoc(
                  obj4inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("C1610"),
                  Some(SubNo("c")),
                  term = "Tusenben"
                ),
                indexMusitObjectDoc(
                  obj7inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("C1610"),
                  Some(SubNo("a"))
                ),
                indexMusitObjectDoc(
                  obj6inCol2,
                  MuseumId(2),
                  Ethnography,
                  MuseumNo("c-402"),
                  isDeleted = true
                ),
                indexSampleDoc(
                  obj6inCol2,
                  sam3FromObj6inCol2,
                  MuseumId(2),
                  isDeleted = true
                )
              ).refresh(RefreshPolicy.IMMEDIATE)
            )
      _ <- indexMaintainer.activateIndex(indexName, objects.indexAlias)
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
      term: String = "maske",
      isDeleted: Boolean = false,
      aggregatedClassData: Option[String] = Some("FullTaxonPath")
  ) = {
    val d = MusitObjectSearch(
      id = id,
      museumId = museumId,
      museumNo = museumNo,
      subNo = subNo,
      term = term,
      mainObjectId = None,
      collection = Some(CollectionSearch(collection)),
      arkForm = None,
      arkFindingNo = None,
      natStage = None,
      natGender = None,
      natLegDate = None,
      isDeleted = isDeleted,
      aggregatedClassData = aggregatedClassData,
      // museumNoPrefix = museumNo.prefix.map(_.toLowerCase),
      museumNoAsANumber = museumNo.asNumber,
      subNoAsANumber = subNo.flatMap(_.asNumber),
      museumNoAsLowerCase = Some(MuseumNo(museumNo.value.toLowerCase)),
      subNoAsLowerCase = subNo.map(s => SubNo(s.value.toLowerCase))
    )
    indexInto(indexName, objects.objectType) id id.underlying.toString doc d
  }

  private def indexSampleDoc(
      fromObject: ObjectUUID,
      sampleId: ObjectUUID,
      museumId: MuseumId,
      isDeleted: Boolean = false
  ) = {
    val d = SampleObjectSearch(
      objectId = Some(sampleId),
      originatedObjectUuid = fromObject,
      parentObject = ParentObject(Some(fromObject), CollectionObjectType),
      isExtracted = false,
      museumId = museumId,
      status = Intact,
      responsible = None,
      doneByStamp = None,
      sampleNum = None,
      sampleId = None,
      externalId = None,
      sampleTypeId = SampleTypeId(1),
      size = None,
      container = None,
      storageMedium = None,
      treatment = None,
      leftoverSample = NoLeftover,
      description = None,
      note = None,
      registeredStamp = None,
      updatedStamp = None,
      isDeleted = isDeleted
    )

    val dId = sampleId.underlying.toString
    val pId = fromObject.underlying.toString
    indexInto(indexName, objects.sampleType) id dId doc d parent pId
  }
}
