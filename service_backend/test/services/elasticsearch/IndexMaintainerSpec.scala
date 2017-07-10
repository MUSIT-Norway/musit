package services.elasticsearch

import akka.NotUsed
import akka.stream.scaladsl.Source
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.JsValue
import services.elasticsearch.client.ElasticsearchClient
import services.elasticsearch.client.models.AliasActions.{
  AddAlias,
  AliasAction,
  DeleteIndex
}
import services.elasticsearch.client.models.{Aliases, BulkActions, RefreshIndex}

import scala.concurrent.Future

class IndexMaintainerSpec extends WordSpec with MustMatchers {

  implicit val ec = scala.concurrent.ExecutionContext.global

  "IndexMaintainer" must {

    "add the new alias to the index" in {
      val es = new DummyEsClient()
      new IndexMaintainer(es).activateIndex("foo_new", "foo")

      es.aliasActions mustBe Seq(AddAlias("foo_new", "foo"))
    }

    "remove the old index when it exists" in {
      val es =
        new DummyEsClient(aliasesResponse = Seq(Aliases("foo_old", Seq("foo"))))
      new IndexMaintainer(es).activateIndex("foo_new", "foo")

      es.aliasActions mustBe Seq(
        AddAlias("foo_new", "foo"),
        DeleteIndex("foo_old")
      )
    }

    "not remove indices that's not prefixed with the alias name" in {
      val es =
        new DummyEsClient(aliasesResponse = Seq(Aliases("baz", Seq("foo"))))
      new IndexMaintainer(es).activateIndex("foo_new", "foo")

      es.aliasActions mustBe Seq(
        AddAlias("foo_new", "foo")
      )
    }

    "move aliases from old to new index " in {
      val es =
        new DummyEsClient(aliasesResponse = Seq(Aliases("foo_old", Seq("foo", "bar"))))
      new IndexMaintainer(es).activateIndex("foo_new", "foo")

      es.aliasActions mustBe Seq(
        AddAlias("foo_new", "foo"),
        AddAlias("foo_new", "bar"),
        DeleteIndex("foo_old")
      )
    }
  }

  def success[T](t: T): Future[MusitResult[T]] = Future.successful(MusitSuccess(t))

  class DummyEsClient(
      var aliasActions: Seq[AliasAction] = Seq(),
      var aliasesResponse: Seq[Aliases] = Seq()
  ) extends ElasticsearchClient {

    override def index(
        index: String,
        tpy: String,
        id: String,
        document: JsValue,
        refresh: RefreshIndex.Refresh
    ) = ???

    override def indices = ???

    override def get(index: String, tpy: String, id: String) = ???

    override def delete(
        index: String,
        tpy: String,
        id: String,
        refresh: RefreshIndex.Refresh
    ) = ???

    override def deleteIndex(index: String) = ???

    override def search(query: String, index: Option[String], typ: Option[String]) = ???

    override def bulkAction(
        source: Source[BulkActions.BulkAction, NotUsed],
        refresh: RefreshIndex.Refresh
    ) = ???

    override def aliases(actions: Seq[AliasAction]) = {
      aliasActions = aliasActions ++ actions
      Future.successful(MusitSuccess(()))
    }

    override def aliases() = Future.successful(MusitSuccess(aliasesResponse))
  }

}
