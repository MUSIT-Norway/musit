package models

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models._
import play.api.libs.json.{Json, Writes}

case class MusitObject(
    id: ObjectId,
    uuid: Option[ObjectUUID],
    museumId: MuseumId,
    museumNo: MuseumNo,
    subNo: Option[SubNo],
    term: String,
    currentLocationId: Option[StorageNodeId] = None,
    path: Option[NodePath] = None,
    pathNames: Option[Seq[NamedPathElement]] = None,
    mainObjectId: Option[Long],
    collection: Option[Collection],
    arkForm: Option[String],
    arkFindingNo: Option[String],
    natStage: Option[String],
    natGender: Option[String],
    natLegDate: Option[String],
    materials: Option[Seq[MusitObjectMaterial]],
    locations: Option[Seq[MusitObjectLocation]]
)

object MusitObject {
  private val baseWrites = Json.writes[MusitObject]

  implicit val writes = Writes[MusitObject] { mo =>
    baseWrites.writes(mo) ++ Json.obj("objectType" -> CollectionObject.name)
  }

  type ObjSearchTuple = (
      (
          Option[ObjectId],
          Option[ObjectUUID],
          MuseumId,
          String,
          Option[Long],
          Option[String],
          Option[Long],
          Option[Long],
          Boolean,
          String,
          Option[String],
          Option[Long],
          Option[Collection],
          Option[String],
          Option[String],
          Option[String],
          Option[String],
          Option[String]
      )
  ) // scalastyle:ignore

  def fromSearchTuple(t: ObjSearchTuple): MusitObject = {
    MusitObject(
      id = t._1.get, // scalastyle:ignore
      uuid = t._2,
      museumId = t._3,
      museumNo = MuseumNo(t._4),
      subNo = t._6.map(SubNo.apply),
      mainObjectId = t._8,
      term = t._10,
      collection = t._13,
      arkForm = t._14,
      arkFindingNo = t._15,
      natStage = t._16,
      natGender = t._17,
      natLegDate = t._18,
      materials = None,
      locations = None
    )
  }

}
