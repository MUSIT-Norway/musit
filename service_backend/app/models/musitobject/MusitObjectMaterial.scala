package models.musitobject

import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import play.api.libs.json.{JsObject, Json, Writes}

sealed trait MusitObjectMaterial {
  val collection: Collection
  val material: Option[String]
}

object MusitObjectMaterial {

  implicit val writes: Writes[MusitObjectMaterial] = Writes {
    case em: EtnoMaterial =>
      EtnoMaterial.writes.writes(em).as[JsObject] ++ Json.obj(
        "collection" -> em.collection
      )
    case am: ArkMaterial =>
      ArkMaterial.writes.writes(am).as[JsObject] ++ Json.obj(
        "collection" -> am.collection
      )
    case nm: NumMaterial =>
      NumMaterial.writes.writes(nm).as[JsObject] ++ Json.obj(
        "collection" -> nm.collection
      )
  }
}

case class EtnoMaterial(
    material: Option[String],
    materialType: Option[String],
    materialElement: Option[String]
) extends MusitObjectMaterial {
  val collection = Ethnography
}

object EtnoMaterial {
  implicit val writes = Json.writes[EtnoMaterial]
}

case class ArkMaterial(
    material: Option[String],
    spesMaterial: Option[String],
    sortering: Option[Int]
) extends MusitObjectMaterial {
  val collection = Archeology
}

object ArkMaterial {
  implicit val writes = Json.writes[ArkMaterial]
}

case class NumMaterial(
    material: Option[String]
) extends MusitObjectMaterial {
  val collection = Numismatics
}

object NumMaterial {
  implicit val writes = Json.writes[NumMaterial]
}
