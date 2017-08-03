package models.analysis

import no.uio.musit.models.ObjectTypes.{CollectionObjectType, ObjectType}
import no.uio.musit.models.ObjectUUID
import play.api.libs.json.Json

case class ParentObject(objectId: Option[ObjectUUID], objectType: ObjectType)

object ParentObject {

  def toParentObject(oid: Option[ObjectUUID], ot: Option[ObjectType]): ParentObject = {
    ParentObject(
      objectId = oid,
      objectType = ot.getOrElse(CollectionObjectType)
    )
  }

  implicit val format = Json.format[ParentObject]

}
