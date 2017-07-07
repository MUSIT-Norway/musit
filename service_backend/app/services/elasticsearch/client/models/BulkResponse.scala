package services.elasticsearch.client.models

import services.elasticsearch.client.models.ItemResponses.ItemResponse
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}

case class BulkResponse(
    took: Int,
    errors: Boolean,
    items: Seq[ItemResponse]
)

object BulkResponse {
  implicit val reads: Reads[BulkResponse] = Json.reads[BulkResponse]
}
object ItemResponses {

  case class Shards(total: Int, successful: Int, failed: Int)

  object Shards {
    implicit val reads: Reads[Shards] = Json.reads[Shards]
  }

  sealed trait ItemResponse {
    def index: String
    def typ: String
    def id: String
    def version: Int
    def result: String
    def shards: Shards
    def status: Int
  }

  object ItemResponse {
    val base = (JsPath \ "_index").read[String] and
      (JsPath \ "_type").read[String] and
      (JsPath \ "_id").read[String] and
      (JsPath \ "_version").read[Int] and
      (JsPath \ "result").read[String] and
      (JsPath \ "_shards").read[Shards] and
      (JsPath \ "status").read[Int]

    implicit val reads: Reads[ItemResponse] = Reads { item =>
      Seq(
        (item \ "index").toOption.map(index => IndexItemResponse.reads.reads(index)),
        (item \ "create").toOption.map(create => CreateItemResponse.reads.reads(create)),
        (item \ "update").toOption.map(update => UpdateItemResponse.reads.reads(update)),
        (item \ "delete").toOption.map(delete => DeleteItemResponse.reads.reads(delete))
      ).flatten.headOption.getOrElse(JsError("No matching action"))
    }
  }

  case class IndexItemResponse(
      index: String,
      typ: String,
      id: String,
      version: Int,
      result: String,
      shards: Shards,
      status: Int,
      created: Boolean
  ) extends ItemResponse

  case class CreateItemResponse(
      index: String,
      typ: String,
      id: String,
      version: Int,
      result: String,
      shards: Shards,
      status: Int,
      created: Boolean
  ) extends ItemResponse

  case class UpdateItemResponse(
      index: String,
      typ: String,
      id: String,
      version: Int,
      result: String,
      shards: Shards,
      status: Int
  ) extends ItemResponse

  case class DeleteItemResponse(
      index: String,
      typ: String,
      id: String,
      version: Int,
      result: String,
      shards: Shards,
      status: Int,
      found: Boolean
  ) extends ItemResponse

  object IndexItemResponse {
    implicit val reads: Reads[IndexItemResponse] =
      (ItemResponse.base and (JsPath \ "created").read[Boolean])(
        IndexItemResponse.apply _
      )
  }

  object CreateItemResponse {
    implicit val reads: Reads[CreateItemResponse] =
      (ItemResponse.base and (JsPath \ "created").read[Boolean])(
        CreateItemResponse.apply _
      )
  }

  object UpdateItemResponse {
    implicit val reads: Reads[UpdateItemResponse] =
      ItemResponse.base(UpdateItemResponse.apply _)
  }

  object DeleteItemResponse {
    implicit val reads: Reads[DeleteItemResponse] =
      (ItemResponse.base and (JsPath \ "found").read[Boolean])(
        DeleteItemResponse.apply _
      )
  }

}
