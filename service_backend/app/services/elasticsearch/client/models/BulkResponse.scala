package services.elasticsearch.client.models

import services.elasticsearch.client.models.ItemResponses.ItemResponse
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
    def status: Int

    def version: Option[Int]
    def result: Option[String]
    def shards: Option[Shards]
    def error: Option[JsValue]

  }

  object ItemResponse {
    val baseExtended = (JsPath \ "_index").read[String] and
      (JsPath \ "_type").read[String] and
      (JsPath \ "_id").read[String] and
      (JsPath \ "status").read[Int] and
      (JsPath \ "_version").readNullable[Int] and
      (JsPath \ "result").readNullable[String] and
      (JsPath \ "_shards").readNullable[Shards] and
      (JsPath \ "error").readNullable[JsValue]

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
      status: Int,
      version: Option[Int],
      result: Option[String],
      shards: Option[Shards],
      error: Option[JsValue],
      created: Option[Boolean]
  ) extends ItemResponse

  case class CreateItemResponse(
      index: String,
      typ: String,
      id: String,
      status: Int,
      version: Option[Int],
      result: Option[String],
      shards: Option[Shards],
      error: Option[JsValue],
      created: Option[Boolean]
  ) extends ItemResponse

  case class UpdateItemResponse(
      index: String,
      typ: String,
      id: String,
      status: Int,
      version: Option[Int],
      result: Option[String],
      shards: Option[Shards],
      error: Option[JsValue]
  ) extends ItemResponse

  case class DeleteItemResponse(
      index: String,
      typ: String,
      id: String,
      status: Int,
      version: Option[Int],
      result: Option[String],
      shards: Option[Shards],
      error: Option[JsValue],
      found: Option[Boolean]
  ) extends ItemResponse

  object IndexItemResponse {
    implicit val reads: Reads[IndexItemResponse] =
      (ItemResponse.baseExtended and (JsPath \ "created").readNullable[Boolean])(
        IndexItemResponse.apply _
      )
  }

  object CreateItemResponse {
    implicit val reads: Reads[CreateItemResponse] =
      (ItemResponse.baseExtended and (JsPath \ "created").readNullable[Boolean])(
        CreateItemResponse.apply _
      )
  }

  object UpdateItemResponse {
    implicit val reads: Reads[UpdateItemResponse] =
      ItemResponse.baseExtended(UpdateItemResponse.apply _)
  }

  object DeleteItemResponse {
    implicit val reads: Reads[DeleteItemResponse] =
      (ItemResponse.baseExtended and (JsPath \ "found").readNullable[Boolean])(
        DeleteItemResponse.apply _
      )
  }

}
