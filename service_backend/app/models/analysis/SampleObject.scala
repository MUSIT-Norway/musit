package models.analysis

import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import no.uio.musit.models.{ActorId, MuseumId, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

case class SampleObject(
    objectId: Option[ObjectUUID],
    originatedObjectUuid: ObjectUUID,
    parentObject: ParentObject,
    isExtracted: Boolean,
    museumId: MuseumId,
    status: SampleStatus,
    responsible: Option[ActorId],
    doneByStamp: Option[ActorStamp],
    sampleNum: Option[Int],
    sampleId: Option[String],
    externalId: Option[ExternalId],
    sampleTypeId: SampleTypeId,
    size: Option[Size],
    container: Option[String],
    storageMedium: Option[String],
    treatment: Option[String],
    leftoverSample: LeftoverSample,
    description: Option[String],
    note: Option[String],
    registeredStamp: Option[ActorStamp],
    updatedStamp: Option[ActorStamp],
    isDeleted: Boolean
)

object SampleObject {

  implicit val writes: Writes[SampleObject] = Json.writes[SampleObject]

}

case class SaveSampleObject(
    parentObject: ParentObject,
    originatedObjectUuid: ObjectUUID,
    isExtracted: Boolean,
    museumId: MuseumId,
    status: SampleStatus,
    doneByStamp: Option[ActorStamp],
    responsible: Option[ActorId],
    doneDate: Option[DateTime],
    sampleId: Option[String],
    externalId: Option[ExternalId],
    sampleTypeId: SampleTypeId,
    size: Option[Size],
    container: Option[String],
    storageMedium: Option[String],
    treatment: Option[String],
    leftoverSample: LeftoverSample,
    description: Option[String],
    note: Option[String]
) {

  def asSampleObject: SampleObject =
    SampleObject(
      objectId = None,
      originatedObjectUuid = originatedObjectUuid,
      parentObject = parentObject,
      isExtracted = isExtracted,
      museumId = museumId,
      status = status,
      responsible = responsible,
      doneByStamp = doneByStamp,
      sampleId = sampleId,
      sampleNum = None,
      externalId = externalId,
      sampleTypeId = sampleTypeId,
      size = size,
      container = container,
      storageMedium = storageMedium,
      treatment = treatment,
      leftoverSample = leftoverSample,
      description = description,
      note = note,
      registeredStamp = None,
      updatedStamp = None,
      isDeleted = false
    )

}

object SaveSampleObject {

  implicit val reads: Reads[SaveSampleObject] = Json.reads[SaveSampleObject]

}
