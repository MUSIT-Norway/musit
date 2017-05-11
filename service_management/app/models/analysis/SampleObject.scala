package models.analysis

import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import no.uio.musit.formatters.DateTimeFormatters._
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, MuseumId, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

case class SampleObject(
    objectId: Option[ObjectUUID],
    originatedObjectUuid: ObjectUUID,
    parentObjectId: Option[ObjectUUID],
    parentObjectType: ObjectType,
    isExtracted: Boolean,
    museumId: MuseumId,
    status: SampleStatus,
    responsible: Option[ActorId],
    doneDate: Option[DateTime],
    sampleNum: Option[Int],
    sampleId: Option[String],
    externalId: Option[ExternalId],
    sampleTypeId: Option[SampleTypeId],
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

  implicit val writes: Writes[SampleObject] = Writes({ so =>
    Json.obj(
      "objectId"             -> Json.toJson(so.objectId),
      "originatedObjectUuid" -> Json.toJson(so.originatedObjectUuid),
      "parentObjectId"       -> Json.toJson(so.parentObjectId),
      "parentObjectType"     -> Json.toJson(so.parentObjectType),
      "isExtracted"          -> Json.toJson(so.isExtracted),
      "museumId"             -> Json.toJson(so.museumId),
      "status"               -> Json.toJson(so.status),
      "responsible"          -> Json.toJson(so.responsible),
      "doneDate"             -> Json.toJson(so.doneDate),
      "sampleNum"            -> Json.toJson(so.sampleNum),
      "sampleId"             -> Json.toJson(so.sampleId),
      "externalId"           -> Json.toJson(so.externalId),
      "sampleTypeId"         -> Json.toJson(so.sampleTypeId),
      "size"                 -> Json.toJson(so.size),
      "container"            -> Json.toJson(so.container),
      "storageMedium"        -> Json.toJson(so.storageMedium),
      "treatment"            -> Json.toJson(so.treatment),
      "leftoverSample"       -> Json.toJson(so.leftoverSample),
      "description"          -> Json.toJson(so.description),
      "note"                 -> Json.toJson(so.note),
      "registeredStamp"      -> Json.toJson(so.registeredStamp),
      "updatedStamp"         -> Json.toJson(so.updatedStamp),
      "isDeleted"            -> Json.toJson(so.isDeleted)
    )
  })

}

case class SaveSampleObject(
    parentObjectId: Option[ObjectUUID],
    originatedObjectUuid: ObjectUUID,
    parentObjectType: ObjectType,
    isExtracted: Boolean,
    museumId: MuseumId,
    status: SampleStatus,
    responsible: Option[ActorId],
    doneDate: Option[DateTime],
    sampleId: Option[String],
    externalId: Option[ExternalId],
    sampleTypeId: Option[SampleTypeId],
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
      parentObjectId = parentObjectId,
      parentObjectType = parentObjectType,
      isExtracted = isExtracted,
      museumId = museumId,
      status = status,
      responsible = responsible,
      doneDate = doneDate,
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
