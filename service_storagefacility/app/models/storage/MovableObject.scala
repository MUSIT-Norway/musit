package models.storage

import no.uio.musit.models.ObjectId
import no.uio.musit.models.ObjectTypes.ObjectType
import play.api.libs.json.{Format, Json}

case class MovableObject(id: ObjectId, objectType: ObjectType)

object MovableObject {
  implicit val format: Format[MovableObject] = Json.format[MovableObject]
}
