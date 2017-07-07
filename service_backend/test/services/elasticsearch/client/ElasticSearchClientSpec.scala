package services.elasticsearch.client

import no.uio.musit.MusitResults.{MusitHttpError, MusitSuccess}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.{BeforeAndAfter, Inside}
import play.api.http.Status
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import services.elasticsearch.client.RefreshIndex.Immediately
import services.elasticsearch.client.models.BulkActions.{
  CreateAction,
  DeleteAction,
  IndexAction,
  UpdateAction
}
import services.elasticsearch.client.models.ItemResponses.{
  CreateItemResponse,
  DeleteItemResponse,
  IndexItemResponse,
  UpdateItemResponse
}

import scala.util.Random

class ElasticSearchClientSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with BeforeAndAfter {
  implicit val format = Json.format[TestUser]

  val client = fromInstanceCache[ElasticSearchClient]
  val index  = s"es-spec-${Random.nextInt(Int.MaxValue)}"

  "ElasticsearchClient" when {

    "using single operation" should {

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
          .delete(
            index = index,
            tpy = "test",
            id = "non_existing",
            refresh = Immediately
          )
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

    }

    "searching" should {
      "find documents" in {
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

    "using bulk operation" should {
      "insert one document" in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(Seq(IndexAction(index, "bulk-test", "1", document)), Immediately)
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1
        val indexItems = res.items.filter(_.isInstanceOf[IndexItemResponse])
        indexItems must have length 1
        indexItems.headOption.map(_.asInstanceOf[IndexItemResponse].result) mustBe
          Some("created")
      }

      "update one document" in {
        insertDoc("1", Json.toJson(TestUser("Ola", "Nordmann", 42)))

        val document = Json.toJson(TestUser("Kari", "Nordmann", 20))
        val res = client
          .bulkAction(
            Seq(UpdateAction(index, "bulk-test", "1", Some(document))),
            Immediately
          )
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1
        val updatedItems = res.items.filter(_.isInstanceOf[UpdateItemResponse])
        updatedItems must have length 1
        updatedItems.headOption.map(_.asInstanceOf[UpdateItemResponse].result) mustBe
          Some("updated")
      }

      "delete one document" in {
        insertDoc("1", Json.toJson(TestUser("Ola", "Nordmann", 42)))
        val res = client
          .bulkAction(Seq(DeleteAction(index, "bulk-test", "1")), Immediately)
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1

        val deletedItems = res.items.filter(_.isInstanceOf[DeleteItemResponse])
        deletedItems.headOption.map(_.asInstanceOf[DeleteItemResponse].result) mustBe
          Some("deleted")
      }

      "create one document" in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(Seq(CreateAction(index, "bulk-test", "1", document)), Immediately)
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1
        val createdItems = res.items.filter(_.isInstanceOf[CreateItemResponse])
        createdItems must have length 1
        createdItems.headOption.map(_.asInstanceOf[CreateItemResponse].result) mustBe
          Some("created")
      }

      "combine multiple actions" in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(
            Seq(
              CreateAction(index, "bulk-test", "1", document),
              CreateAction(index, "bulk-test", "2", document),
              UpdateAction(index, "bulk-test", "1", Some(document)),
              DeleteAction(index, "bulk-test", "3"),
              IndexAction(index, "bulk-test", "4", document),
              IndexAction(index, "bulk-test", "5", document),
              IndexAction(index, "bulk-test", "6", document)
            ),
            Immediately
          )
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 7
      }

      def insertDoc(id: String, doc: JsValue) = {
        client
          .index(
            index = index,
            tpy = "bulk-test",
            id = id,
            document = doc,
            refresh = Immediately
          )
          .futureValue
          .successValue
      }

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
    client.jsonClient(index).put(createIndex).futureValue
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
