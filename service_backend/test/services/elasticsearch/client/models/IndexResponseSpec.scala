package services.elasticsearch.client.models

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class IndexResponseSpec extends WordSpec with MustMatchers {
  "IndexResponseSpec" must {
    "parse from json" in {
      val json =
        """{
             "docs.count": "3",
             "docs.deleted": "0",
             "health": "yellow",
             "index": "es-spec",
             "pri": "2",
             "pri.store.size": "12.5kb",
             "rep": "1",
             "status": "open",
             "store.size": "12.5kb",
             "uuid": "9gsuqaf3RBaGg-7I5pZ0hg"
           }"""

      val valid = Json.parse(json).validate[IndexResponse]

      valid.isSuccess mustBe true
    }
  }
}
