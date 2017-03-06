package models

import models.SampleStatuses.SampleStatus
import no.uio.musit.models.{ActorId, MuseumId, ObjectUUID}
import org.joda.time.DateTime

case class SampleObject(
  objectId: Option[ObjectUUID],
  parentObjectId: Option[ObjectUUID],
  isCollectionObject: Boolean,
  museumId: MuseumId,
  status: SampleStatus,
  responsible: ActorId,
  createdDate: DateTime,
  sampleNumber: Option[String],
  externalId: Option[String],
  note: Option[String],
  updatedBy: Option[ActorId],
  updatedDate: Option[DateTime]
)

object SampleObject {

}
