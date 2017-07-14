package services.elasticsearch.client.models

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsDefined, JsSuccess, Json}

class ElasticsearchConfigSpec extends WordSpec with MustMatchers {

  "ElasticsearchConfig" should {
    "create mapping with an index" in {
      val mappings =
        Json.toJson(ElasticsearchConfig(Set(IndexMapping("myIndex", Set.empty))))

      (mappings \ "mappings") mustBe a[JsDefined]
      (mappings \ "mappings" \ "myIndex") mustBe a[JsDefined]
    }

    "create mapping with properties" in {
      val mappings = Json.toJson(
        ElasticsearchConfig(
          Set(
            IndexMapping(
              "myIndex",
              Set(
                IntegerField("myInt"),
                TextField("myText"),
                KeywordField("myKeyword"),
                DateField("myDate", Some("yyyy-dd-mm")),
                NestedField("myArray", Set(TextField("myText"))),
                ObjectField(
                  "myObject",
                  Set(
                    IntegerField("childInt"),
                    TextField("childText")
                  )
                )
              )
            )
          )
        )
      )

      (mappings \ "mappings" \ "myIndex" \ "properties" \ "myInt" \ "type")
        .validateOpt[String] mustBe JsSuccess(Some("integer"))

      (mappings \ "mappings" \ "myIndex" \ "properties" \ "myDate" \ "format")
        .validateOpt[String] mustBe JsSuccess(Some("yyyy-dd-mm"))
    }
  }

}
