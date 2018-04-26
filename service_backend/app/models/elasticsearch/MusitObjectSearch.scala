package models.elasticsearch

import java.util.UUID

import models.musitobject.MusitObject
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.ObjectTypes.CollectionObjectType
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
    natLegDate: Option[String],
    isDeleted: Boolean,
    aggregatedClassData: Option[String],
    // museumNoPrefix: Option[String],
    museumNoAsANumber: Option[Long],
    subNoAsANumber: Option[Long],
    museumNoAsLowerCase: Option[MuseumNo],
    subNoAsLowerCase: Option[SubNo]
) extends Searchable {
  override val docId       = id.underlying.toString
  override val docParentId = None
}

case class CollectionSearch(id: Int, uuid: UUID)

object CollectionSearch {
  implicit val writes: Writes[CollectionSearch] = Json.writes[CollectionSearch]

  def apply(c: Collection): CollectionSearch = CollectionSearch(c.id, c.uuid.underlying)
}

object MusitObjectSearch {
  private val baseWrites = Json.writes[MusitObjectSearch]

  implicit val writes = Writes[MusitObjectSearch] { mo =>
    baseWrites.writes(mo) ++ Json.obj("objectType" -> CollectionObjectType.name)
  }

  def apply(mo: MusitObject): MusitObjectSearch = MusitObjectSearch(
    mo.uuid.get,
    mo.museumId,
    mo.museumNo,
    mo.subNo,
    mo.term,
    mo.mainObjectId,
    mo.collection.map(CollectionSearch.apply),
    mo.arkForm,
    mo.arkFindingNo,
    mo.natStage,
    mo.natGender,
    mo.natLegDate,
    mo.isDeleted,
    mo.aggregatedClassData,
    //mo.museumNo.prefix.map(_.toLowerCase),
    mo.museumNo.asNumber,
    mo.subNo.flatMap(_.asNumber),
    Some(MuseumNo(mo.museumNo.value.toLowerCase)),
    mo.subNo.map(s => SubNo(s.value.toLowerCase))
  )
}
