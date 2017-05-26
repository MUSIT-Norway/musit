package models.analysis

import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.ObjectUUID
import play.api.libs.json.Json

case class ParentObject(objectId: Option[ObjectUUID], objectType: ObjectType)

object ParentObject {

  implicit val format = Json.format[ParentObject]

}
