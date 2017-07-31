package services.elasticsearch.client.models

import org.scalatest.{Inside, MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}

class BulkResponseSpec extends WordSpec with MustMatchers with Inside {

  "BulkResponse" must {
    "parse success response" in {
      val successResponse =
        """
        |{
        |  "took":274,
        |  "errors":true,
        |  "items":[
        |    {
        |      "index":{
        |        "_index":"events_1501491671087",
        |        "_type":"analysisCollection",
        |        "_id":"1",
        |        "_version":1,
        |        "result":"created",
        |        "_shards":{
        |          "total":2,
        |          "successful":1,
        |          "failed":0
        |        },
        |        "created":true,
        |        "status":201
        |      }
        |    }
        |  ]
        |}
        |
      """.stripMargin

      val res = Json.parse(successResponse).validate[BulkResponse]

      inside(res) {
        case JsSuccess(response, _) =>
          response.items must have length 1
          response.items.filter(_.error.isEmpty) must have length 1
      }

    }

    "parse error as object" in {
      val errorResponse =
        """
          |{
          |  "took":274,
          |  "errors":true,
          |  "items":[
          |    {
          |      "index":{
          |        "_index":"events_1501491671087",
          |        "_type":"analysis",
          |        "_id":"2",
          |        "status":400,
          |        "error":{
          |          "type":"routing_missing_exception",
          |          "reason":"routing is required for [events_1501491671087]/[analysis]/[2]",
          |          "index_uuid":"_na_",
          |          "index":"events_1501491671087"
          |        }
          |      }
          |    }
          |  ]
          |}
      """.stripMargin

      val res = Json.parse(errorResponse).validate[BulkResponse]

      inside(res) {
        case JsSuccess(response, _) =>
          response.items must have length 1
          response.items.filter(_.error.isDefined) must have length 1
      }
    }

    "parse error as string" in {
      val errorResponse =
        """
          |{
          |  "took":274,
          |  "errors":true,
          |  "items":[
          |    {
          |      "index":{
          |        "_index":"events_1501491671087",
          |        "_type":"analysis",
          |        "_id":"2",
          |        "status":400,
          |        "error":"foo"
          |      }
          |    }
          |  ]
          |}
        """.stripMargin

      val res = Json.parse(errorResponse).validate[BulkResponse]

      inside(res) {
        case JsSuccess(response, _) =>
          response.items must have length 1
          response.items.filter(_.error.isDefined) must have length 1
      }
    }

  }

}
