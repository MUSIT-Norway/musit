package services.elasticsearch.client

import no.uio.musit.MusitResults.{MusitHttpError, MusitSuccess}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.{BeforeAndAfter, Inside}
import play.api.http.Status
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import services.elasticsearch.client.RefreshIndex.Immediately

import scala.util.Random

class ElasticSearchClientSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with BeforeAndAfter {
  implicit val format = Json.format[TestUser]

  val client = fromInstanceCache[ElasticSearchClient]
  val index  = s"es-spec-${Random.nextInt(Int.MaxValue)}"

  "ElasticsearchClient" must {
    "insert document into index" in {
      val doc = TestUser("Ola", " Nordmann", 42)

      val result = client
        .index(
          index = index,
          tpy = "test",
          id = "1",
          document = Json.toJson(doc),
          refresh = Immediately
        )
        .futureValue

      result mustBe a[MusitSuccess[_]]
      (result.get \ "_version").as[Int] mustBe 1
    }

    "list out created index" in {
      val doc = TestUser("Ola", " Nordmann", 42)

      client
        .index(
          index = index,
          tpy = "test",
          id = "1",
          document = Json.toJson(doc),
          refresh = Immediately
        )
        .futureValue
      val result = client.indices.futureValue

      result.successValue.map(_.index) must contain(index)
    }

    "delete index when it exist" in {
      val doc = TestUser("Ola", " Nordmann", 42)

      client
        .index(
          index = index,
          tpy = "test",
          id = "1",
          document = Json.toJson(doc),
          refresh = Immediately
        )
        .futureValue
      val res = client.deleteIndex(index).futureValue.successValue

      (res \ "acknowledged").as[Boolean] mustBe true
      val result = client.indices.futureValue

      result.successValue.map(_.index) must not contain index
    }

    "delete non existing index" in {
      val res =
        client.deleteIndex("some-random-index").futureValue

      inside(res) {
        case MusitHttpError(code, _) => code mustBe Status.NOT_FOUND
      }
    }

    "delete document" in {
      val doc = TestUser("Ola", " Nordmann", 42)
      val tpy = "test"
      val id  = "to_delete"
      client
        .index(
          index = index,
          tpy = tpy,
          id = id,
          document = Json.toJson(doc),
          refresh = Immediately
        )
        .futureValue

      val res = client
        .delete(index = index, tpy = tpy, id = id, refresh = Immediately)
        .futureValue
        .successValue

      (res \ "found").as[Boolean] mustBe true

      val document =
        client.get(index = index, tpy = tpy, id = id).futureValue.successValue
      document mustBe None
    }

    "delete non existing document" in {
      val doc = TestUser("Ola", " Nordmann", 42)

      val res = client
        .delete(index = index, tpy = "test", id = "non_existing", refresh = Immediately)
        .futureValue

      inside(res) {
        case MusitHttpError(code, _) => code mustBe Status.NOT_FOUND
      }
    }

    "update existing document into index" in {
      val docV1 = TestUser("Ola", "Nordmann", 42)
      val docV2 = TestUser("Ola", "Nordmann", 43)

      val firstResult = client
        .index(
          index = index,
          tpy = "test",
          id = "2",
          document = Json.toJson(docV1),
          refresh = Immediately
        )
        .futureValue
      firstResult mustBe a[MusitSuccess[_]]

      val secondResult =
        client
          .index(
            index = index,
            tpy = "test",
            id = "2",
            document = Json.toJson(docV2),
            refresh = Immediately
          )
          .futureValue

      secondResult mustBe a[MusitSuccess[_]]
      (secondResult.get \ "_version").as[Int] mustBe 2
    }

    "retrieve inserted document" in {
      val doc = TestUser("Ola", "Nordmann", 42)

      val result =
        client
          .index(
            index = index,
            tpy = "test",
            id = "1",
            document = Json.toJson(doc),
            refresh = Immediately
          )
          .futureValue
      result mustBe a[MusitSuccess[_]]

      val document = client.get(index = index, tpy = "test", id = "1").futureValue
      document mustBe a[MusitSuccess[_]]

      (document.get.get \ "_source").get.as[TestUser] mustBe doc
    }

    "retrieve non existing document" in {
      val document = client.get(index, "test", "42").futureValue
      document mustBe a[MusitSuccess[_]]

      document.get mustBe empty
    }

    "search for documents" in {
      client
        .index(
          index = index,
          tpy = "test",
          id = "1",
          document = Json.toJson(TestUser("Ola", "Nordmann", 42)),
          refresh = Immediately
        )
        .futureValue
      client
        .index(
          index = index,
          tpy = "test",
          id = "2",
          document = Json.toJson(TestUser("Kari", "Nordmann", 32)),
          refresh = Immediately
        )
        .futureValue
      client
        .index(
          index = index,
          tpy = "test",
          id = "3",
          document = Json.toJson(TestUser("Pal", "Svendsen", 45)),
          refresh = Immediately
        )
        .futureValue

      val result = client.search("lastName:Nordmann", Some(index), None).futureValue

      result mustBe a[MusitSuccess[_]]
      (result.get \ "hits" \ "total").as[Int] mustBe 2
    }

  }

  before {
    val createIndex: JsValue = JsObject(
      Map(
        "settings" -> JsObject(
          Map(
            "index" -> JsObject(
              Map(
                "number_of_shards"   -> JsNumber(2),
                "number_of_replicas" -> JsNumber(1)
              )
            )
          )
        )
      )
    )
    client.client(index).put(createIndex).futureValue
  }

  after {
    client.deleteIndex(index).futureValue
  }
}

case class TestUser(
    firstName: String,
    lastName: String,
    age: Int
)
