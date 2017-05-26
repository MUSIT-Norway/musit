package models.reporting

import no.uio.musit.models.CollectionUUID
import play.api.libs.json.{Format, Json}

case class CollectionStats(uuid: CollectionUUID, numObjects: Int = 0)

object CollectionStats {
  implicit val format: Format[CollectionStats] = Json.format[CollectionStats]
}

case class NodeStats(
    numNodes: Int = 0,
    numObjects: Int = 0,
    totalObjects: Int = 0
)

object NodeStats {
  implicit val format: Format[NodeStats] = Json.format[NodeStats]
}
