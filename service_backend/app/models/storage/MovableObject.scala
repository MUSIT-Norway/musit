package models.storage

import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.ObjectUUID
import play.api.libs.json.{Format, Json}

case class MovableObject(id: ObjectUUID, objectType: ObjectType)

object MovableObject {
  implicit val format: Format[MovableObject] = Json.format[MovableObject]
}
