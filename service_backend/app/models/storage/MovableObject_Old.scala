package models.storage

import no.uio.musit.models.ObjectId
import no.uio.musit.models.ObjectTypes.ObjectType
import play.api.libs.json.{Format, Json}

// TODO: DELETE ME when Migration is performed in production

case class MovableObject_Old(id: ObjectId, objectType: ObjectType)

object MovableObject_Old {
  implicit val format: Format[MovableObject_Old] = Json.format[MovableObject_Old]
}
