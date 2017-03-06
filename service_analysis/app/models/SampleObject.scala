package models

import models.SampleStatuses.SampleStatus
import no.uio.musit.models.{ActorId, ObjectId}
import org.joda.time.DateTime

case class SampleObject(
  objectId: Option[ObjectId],
  parentObjectId: ObjectId,
  isCollectionObject: Boolean,
  status: SampleStatus,
  responsible: ActorId,
  createdDate: DateTime,
  sampleNumber: Option[String],
  externalId: Option[String],
  note: Option[String]
)

object SampleObject {

}
