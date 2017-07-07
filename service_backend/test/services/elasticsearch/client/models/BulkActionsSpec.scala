package services.elasticsearch.client.models

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsString, JsValue, Json}
import services.elasticsearch.client.models.BulkActions._

class BulkActionsSpec extends WordSpec with MustMatchers with OptionValues {
  val content = Json.obj("key" -> JsString("value"))
  val index   = "ind"
  val typ     = "typ"
  val id      = "id"

  "Bulk actions" must {

    "create json from Index action" in {
      val res = Json.toJson(IndexAction(index, typ, id, content))
      verifyAction("index", res)
    }

    "create json from Create action" in {
      val res = Json.toJson(CreateAction(index, typ, id, content))
      verifyAction("create", res)
    }

    "create json from Update action" in {
      val res = Json.toJson(UpdateAction(index, typ, id, Some(content)))
      verifyAction("update", res)
    }

    "create json from Delete action" in {
      val res = Json.toJson(DeleteAction(index, typ, id))
      verifyAction("delete", res)
    }
  }

  def verifyAction(action: String, res: JsValue) = {
    val details = (res \ action).toOption.value

    (details \ "_index").as[String] mustBe index
    (details \ "_type").as[String] mustBe typ
    (details \ "_id").as[String] mustBe id
  }

}
