package models.musitobject

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.ObjectTypes.CollectionObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}
import play.api.Logger

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
    numismaticAttribute: Option[NumismaticsAttribute],
    materials: Option[Seq[MusitObjectMaterial]],
    locations: Option[Seq[MusitObjectLocation]],
    coordinates: Option[Seq[MusitObjectCoordinate]],
    isDeleted: Boolean,
    aggregatedClassData: Option[String]
)

case class NumismaticsAttribute(
    denotation: Option[String],
    valor: Option[String],
    date: Option[String],
    weight: Option[String]
)

object NumismaticsAttribute {
  implicit val writes = Json.writes[NumismaticsAttribute]
}

object MusitObject {
  private val baseWrites = Json.writes[MusitObject]

  implicit val writes = Writes[MusitObject] { mo =>
    baseWrites.writes(mo) ++ Json.obj("objectType" -> CollectionObjectType.name)
  }

  private[this] val logger = Logger(classOf[MusitObject])

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
          Option[String],
          (Option[String], Option[String], Option[String], Option[String]),
          DateTime,
          Option[String]
      )
  ) // scalastyle:ignore

  def fromSearchTuple(t: ObjSearchTuple): MusitObject = {
    logger.info(s"fromSearcTuple: id = ${t._1} uuid: ${t._2}")
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
      numismaticAttribute =
        if (t._19._1.isDefined ||
            t._19._2.isDefined ||
            t._19._3.isDefined ||
            t._19._4.isDefined)
          Some(
            NumismaticsAttribute(
              denotation = t._19._1,
              valor = t._19._2,
              date = t._19._3,
              weight = t._19._4
            )
          )
        else None,
      materials = None,
      locations = None,
      coordinates = None,
      isDeleted = t._9,
      aggregatedClassData = t._21
    )
  }

}
