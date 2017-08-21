package models.elasticsearch

import java.util.UUID

import models.musitobject.MusitObject
import no.uio.musit.models.{MuseumId, MuseumNo, ObjectUUID, SubNo}
import play.api.libs.json.{Json, Writes}

case class MusitObjectSearch(
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
) extends Searchable {
  override val docId       = id.underlying.toString
  override val docParentId = None
  val documentType: String = "collection"
}

case class CollectionSearch(id: Int, uuid: UUID)

object CollectionSearch {
  implicit val writes: Writes[CollectionSearch] = Json.writes[CollectionSearch]
}

object MusitObjectSearch {
  implicit val writes: Writes[MusitObjectSearch] = Json.writes[MusitObjectSearch]

  def apply(mo: MusitObject): MusitObjectSearch = MusitObjectSearch(
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
