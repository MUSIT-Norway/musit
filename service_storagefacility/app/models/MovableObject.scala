package models

import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.ObjectId
import play.api.libs.json.{Format, Json}

case class MovableObject(id: ObjectId, objectType: ObjectType)

object MovableObject {
  implicit val format: Format[MovableObject] = Json.format[MovableObject]
}
