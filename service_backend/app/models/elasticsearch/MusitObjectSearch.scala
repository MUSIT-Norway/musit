package models.elasticsearch

import java.util.UUID

import models.musitobject.MusitObject
import no.uio.musit.models.{MuseumId, MuseumNo, ObjectUUID, SubNo}
import play.api.libs.json.{Json, Writes}

sealed trait MusitObjectSearch {
  val documentId: String
  val documentType: String
}

object MusitObjectSearch {
  implicit val writes: Writes[MusitObjectSearch] = Writes[MusitObjectSearch] {
    case obj: MustObjectSearch => MustObjectSearch.writes.writes(obj)
  }
}

case class MustObjectSearch(
    id: ObjectUUID,
    museumId: MuseumId,
    museumNo: MuseumNo,
    subNo: Option[SubNo],
    term: String,
    mainObjectId: Option[Long],
    collection: Option[CollectionSearch],
    arkForm: Option[String],
    arkFindingNo: Option[String],
    natStage: Option[String],
    natGender: Option[String],
    natLegDate: Option[String]
) extends MusitObjectSearch {
  override val documentId   = id.underlying.toString
  override val documentType = "collection"
}

case class CollectionSearch(id: Int, uuid: UUID)

object CollectionSearch {
  implicit val writes: Writes[CollectionSearch] = Json.writes[CollectionSearch]
}

object MustObjectSearch {
  val writes: Writes[MustObjectSearch] = Json.writes[MustObjectSearch]

  def apply(mo: MusitObject): MustObjectSearch = MustObjectSearch(
    mo.uuid.get,
    mo.museumId,
    mo.museumNo,
    mo.subNo,
    mo.term,
    mo.mainObjectId,
    mo.collection.map(c => CollectionSearch(c.id, c.uuid.underlying)),
    mo.arkForm,
    mo.arkFindingNo,
    mo.natStage,
    mo.natGender,
    mo.natLegDate
  )
}
