package services.elasticsearch.index

import com.sksamuel.elastic4s.http.cat.{CatAlias, Routing}
import org.scalatest.{MustMatchers, WordSpec}
import services.elasticsearch.index.IndexMaintainer._

class IndexMaintainerSpec extends WordSpec with MustMatchers {

  implicit val ec = scala.concurrent.ExecutionContext.global

  "IndexMaintainer" should {

    def catAlias(alias: String, index: String) = CatAlias(
      alias,
      index,
      "-",
      Routing("-", "-")
    )

    "find indices to delete" in {
      val res = findIndicesToDelete(
        Seq(
          catAlias("foo", "foo-1"),
          catAlias("foo", "foo-2"),
          catAlias("bar", "foo-1"),
          catAlias("baz", "bar-1")
        ),
        "foo"
      )

      res mustBe Set("foo-1", "foo-2")
    }

    "find alias to move" in {
      val res = findAliasesToMove(
        Seq(
          catAlias("foo", "foo-1"),
          catAlias("bar", "foo-1"),
          catAlias("doh", "foo-1"),
          catAlias("baz", "bar-1")
        ),
        "foo"
      )

      res mustBe Set("bar", "doh")
    }
  }

}
