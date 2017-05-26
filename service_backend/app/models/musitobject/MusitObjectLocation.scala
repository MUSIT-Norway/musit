package models.musitobject

import no.uio.musit.models.MuseumCollections.{Algae, Archeology, Collection, Ethnography}
import play.api.libs.json.{JsObject, Json, Writes}

sealed trait MusitObjectLocation {
  val collection: Collection
}

object MusitObjectLocation {

  implicit val writes: Writes[MusitObjectLocation] = Writes {
    case el: EtnoLocation =>
      EtnoLocation.writes.writes(el).as[JsObject] ++ Json.obj(
        "collection" -> el.collection
      )
    case al: ArkLocation =>
      ArkLocation.writes.writes(al).as[JsObject] ++ Json.obj(
        "collection" -> al.collection
      )

    case nl: NatLocation =>
      NatLocation.writes.writes(nl).as[JsObject] ++ Json.obj(
        "collection" -> nl.collection
      )
  }
}

case class EtnoLocation(country: Option[String]) extends MusitObjectLocation {
  val collection = Ethnography
}

object EtnoLocation {
  implicit val writes = Json.writes[EtnoLocation]
}

case class ArkLocation(
    farmName: Option[String],
    farmNo: Option[Int],
    propertyUnitNo: Option[String]
) extends MusitObjectLocation {
  val collection = Archeology
}

object ArkLocation {
  implicit val writes = Json.writes[ArkLocation]
}

case class NatLocation(
    natCountry: Option[String],
    natStateProv: Option[String],
    natMunicipality: Option[String],
    natLocality: Option[String],
    natCoordinate: Option[String],
    natCoordDatum: Option[String],
    natSoneBand: Option[String]
) extends MusitObjectLocation {
  val collection = Algae //selected one of the collection with Nature extended trait.
}

object NatLocation {
  implicit val writes = Json.writes[NatLocation]
}
