package models.musitobject

import no.uio.musit.models.MuseumCollections.{Archeology, Collection}
import play.api.libs.json.{JsObject, Json, Writes}

sealed trait MusitObjectCoordinate {
  val collection: Collection
}

object MusitObjectCoordinate {

  implicit val writes: Writes[MusitObjectCoordinate] = Writes {

    case al: ArkCoordinate =>
      ArkCoordinate.writes.writes(al).as[JsObject] ++ Json.obj(
        "collection" -> al.collection
      )
  }
}

case class ArkCoordinate(
    projection: Option[String],
    precision: Option[String],
    north: Option[String],
    east: Option[String]
) extends MusitObjectCoordinate {
  val collection = Archeology
}

object ArkCoordinate {
  implicit val writes = Json.writes[ArkCoordinate]
}
