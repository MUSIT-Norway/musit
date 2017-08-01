package services.elasticsearch.client

import akka.stream.scaladsl.Source
import no.uio.musit.MusitResults.{MusitHttpError, MusitSuccess}
import no.uio.musit.test
import no.uio.musit.test.{ElasticsearchContainer, MusitSpecWithAppPerSuite}
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, Inside}
import play.api.http.Status
import play.api.libs.json._
import services.elasticsearch.client.models.AliasActions.{
  AddAlias,
  DeleteIndex,
  RemoveAlias
}
import services.elasticsearch.client.models.{
  Aliases,
  IndexMapping,
  ElasticsearchConfig,
  TextField
}
import services.elasticsearch.client.models.RefreshIndex.Immediately
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

class ElasticsearchHttpClientSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside
    with Eventually
    with BeforeAndAfter {
  implicit val format = Json.format[TestUser]

  val client = fromInstanceCache[ElasticsearchHttpClient]

  var index: String = _

  before {
    index = s"es-spec-${Random.nextInt(Int.MaxValue)}"
  }

  "ElasticsearchClient" when {

    "using single operation" should {

      "insert document into index" taggedAs ElasticsearchContainer in {
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

      "find all indices" taggedAs ElasticsearchContainer in {
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

      "delete existing index" taggedAs ElasticsearchContainer in {
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

      "report error when deleting index that doesn't exist" taggedAs ElasticsearchContainer in {
        val res =
          client.deleteIndex("some-random-index").futureValue

        inside(res) {
          case MusitHttpError(code, _) => code mustBe Status.NOT_FOUND
        }
      }

      "delete existing document" taggedAs ElasticsearchContainer in {
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

      "report error when delete non existing document" taggedAs ElasticsearchContainer in {
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

      "update existing document into index" taggedAs ElasticsearchContainer in {
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

      "retrieve inserted document" taggedAs ElasticsearchContainer in {
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

      "not fail when retriving document that doens't exist" taggedAs ElasticsearchContainer in {
        val document = client.get(index, "test", "42").futureValue
        document mustBe a[MusitSuccess[_]]

        document.get mustBe empty
      }

    }

    "searching" should {
      "find documents matching query" taggedAs ElasticsearchContainer in {
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
      "insert one document" taggedAs ElasticsearchContainer in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(
            Source(List(IndexAction(index, "bulk-test", "1", document))),
            Immediately
          )
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1
        val indexItems = res.items.filter(_.isInstanceOf[IndexItemResponse])
        indexItems must have length 1
        indexItems.headOption.map(_.asInstanceOf[IndexItemResponse].result) mustBe
          Some("created")
      }

      "update one document" taggedAs ElasticsearchContainer in {
        insertDoc("1", Json.toJson(TestUser("Ola", "Nordmann", 42)))

        val document = Json.toJson(TestUser("Kari", "Nordmann", 20))
        val res = client
          .bulkAction(
            Source(List(UpdateAction(index, "bulk-test", "1", Some(document)))),
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

      "delete one document" taggedAs ElasticsearchContainer in {
        insertDoc("1", Json.toJson(TestUser("Ola", "Nordmann", 42)))
        val res = client
          .bulkAction(Source(List(DeleteAction(index, "bulk-test", "1"))), Immediately)
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1

        val deletedItems = res.items.filter(_.isInstanceOf[DeleteItemResponse])
        deletedItems.headOption.map(_.asInstanceOf[DeleteItemResponse].result) mustBe
          Some("deleted")
      }

      "create one document" taggedAs ElasticsearchContainer in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(
            Source(List(CreateAction(index, "bulk-test", "1", document))),
            Immediately
          )
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 1
        val createdItems = res.items.filter(_.isInstanceOf[CreateItemResponse])
        createdItems must have length 1
        createdItems.headOption.map(_.asInstanceOf[CreateItemResponse].result) mustBe
          Some("created")
      }

      "combine multiple actions" taggedAs ElasticsearchContainer in {
        val document = Json.toJson(TestUser("Ola", "Nordmann", 42))
        val res = client
          .bulkAction(
            Source(
              List(
                CreateAction(index, "bulk-test", "1", document),
                CreateAction(index, "bulk-test", "2", document),
                UpdateAction(index, "bulk-test", "1", Some(document)),
                DeleteAction(index, "bulk-test", "3"),
                IndexAction(index, "bulk-test", "4", document),
                IndexAction(index, "bulk-test", "5", document),
                IndexAction(index, "bulk-test", "6", document)
              )
            ),
            Immediately
          )
          .futureValue
          .successValue

        res.errors mustBe false
        res.items must have length 7
      }

    }

    "operation on aliases" should {
      "create alias" taggedAs ElasticsearchContainer in {
        client.aliases(Seq(AddAlias("foo", "baz"))).futureValue.successValue
      }

      "remove alias" taggedAs ElasticsearchContainer in {
        client.aliases(Seq(AddAlias("foo", "baz"))).futureValue
        client.aliases(Seq(RemoveAlias("foo", "baz"))).futureValue.successValue
      }

      "delete index via alias action" taggedAs ElasticsearchContainer in {
        client.aliases(Seq(DeleteIndex("foo"))).futureValue.successValue
      }

      "list all aliases" taggedAs test.ElasticsearchContainer in {
        insertDoc("alias_test", Json.obj("foo" -> JsString("Bar")))
        client.aliases(Seq(AddAlias(index, "bar"), AddAlias(index, "baz"))).futureValue

        eventually {
          val res = client.aliases.futureValue.successValue
          res.find(_.index == index) mustBe Some(Aliases(index, Seq("bar", "baz")))
        }
      }
    }

    "config" should {
      "insert mapping on new index" taggedAs ElasticsearchContainer in {
        val doc = Json.toJson(TestUser("Ola", "Nordmann", 42))

        val mappings = ElasticsearchConfig(
          Set(
            IndexMapping(
              name = "foo",
              properties = Set(
                TextField("firstName"),
                TextField("lastName")
              )
            )
          )
        )
        client.config(index, mappings).futureValue.successValue

        client
          .bulkAction(Source(List(IndexAction(index, "foo", "1", doc))))
          .futureValue
          .successValue
      }

      "insert mapping on new index with parent" taggedAs ElasticsearchContainer in {
        val doc = Json.toJson(TestUser("Ola", "Nordmann", 42))

        val mappings = ElasticsearchConfig(
          Set(
            IndexMapping(
              name = "foo",
              properties = Set(
                TextField("firstName"),
                TextField("lastName")
              ),
              parent = Some("baz")
            ),
            IndexMapping(
              name = "baz",
              properties = Set(
                TextField("firstName"),
                TextField("lastName")
              )
            )
          )
        )
        client.config(index, mappings).futureValue.successValue

        client
          .bulkAction(
            Source(
              List(
                IndexAction(index, "foo", "1", doc, Some("2")),
                IndexAction(index, "bar", "2", doc)
              )
            )
          )
          .futureValue
          .successValue
      }
    }
  }

  def insertDoc(id: String, doc: JsValue, typ: String = "bulk-test") = {
    client
      .index(
        index = index,
        tpy = typ,
        id = id,
        document = doc,
        refresh = Immediately
      )
      .futureValue
      .successValue
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
