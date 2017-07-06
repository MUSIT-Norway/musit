package services.elasticsearch.client.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class IndexResponse(
    docsCount: Int,
    docsDeleted: Int,
    health: String,
    index: String,
    pri: String,
    priStoreSize: String,
    rep: Int,
    status: String,
    storeSize: String,
    uuid: String
)

object IndexResponse {
  implicit val reads: Reads[IndexResponse] = (
    (JsPath \ "docs.count").read[String].map(_.toInt) and
      (JsPath \ "docs.deleted").read[String].map(_.toInt) and
      (JsPath \ "health").read[String] and
      (JsPath \ "index").read[String] and
      (JsPath \ "pri").read[String] and
      (JsPath \ "pri.store.size").read[String] and
      (JsPath \ "rep").read[String].map(_.toInt) and
      (JsPath \ "status").read[String] and
      (JsPath \ "store.size").read[String] and
      (JsPath \ "uuid").read[String]
  )(IndexResponse.apply _)
}
