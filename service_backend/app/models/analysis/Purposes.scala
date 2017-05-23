package models.analysis

import play.api.libs.json.{JsObject, Json}

object Purposes {

  sealed trait Purpose { self =>
    val id: Int
    val noPurpose: String
    val enPurpose: String
  }

  val purposes = List(
    MaterialDetermination,
    Dating,
    ProvenanceDetermination,
    ContructionUnderstanding
  )

  object Purpose {

    def purposeIdToNoString(i: Int) =
      fromInt(i).map(_.noPurpose).getOrElse("")

    def purposeIdToEnString(i: Int) =
      fromInt(i).map(_.enPurpose).getOrElse("")

    def fromInt(i: Int): Option[Purpose] = purposes.find(_.id == i)

    def fromString(s: String): Option[Purpose] =
      purposes.find(p => p.enPurpose == s || p.noPurpose == s)

    def toJson(m: Purpose): JsObject = {
      Json.obj(
        "id"        -> m.id,
        "noPurpose" -> m.noPurpose,
        "enPurpose" -> m.enPurpose
      )
    }

  }

  case object MaterialDetermination extends Purpose {
    override val id                = 1
    override val noPurpose: String = "Materialbestemmelse"
    override val enPurpose: String = "Material determination"
  }

  case object Dating extends Purpose {
    override val id                = 2
    override val noPurpose: String = "Datering"
    override val enPurpose: String = "Dating"
  }

  case object ProvenanceDetermination extends Purpose {
    override val id                = 3
    override val noPurpose: String = "Proveniensbestemmelse"
    override val enPurpose: String = "Provenance determination"
  }

  case object ContructionUnderstanding extends Purpose {
    override val id                = 4
    override val noPurpose: String = "Konstruksjonsforståelse"
    override val enPurpose: String = "Construction understanding"
  }

}
