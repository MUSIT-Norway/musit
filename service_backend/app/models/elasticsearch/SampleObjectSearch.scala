package models.elasticsearch

import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis._
import models.elasticsearch.Actors.{ActorSearch, ActorSearchStamp}
import no.uio.musit.models.{MuseumId, ObjectUUID}
import play.api.libs.json.{Json, Writes}

case class SampleObjectSearch(
    objectId: Option[ObjectUUID],
    originatedObjectUuid: ObjectUUID,
    parentObject: ParentObject,
    isExtracted: Boolean,
    museumId: MuseumId,
    status: SampleStatus,
    responsible: Option[ActorSearch],
    doneByStamp: Option[ActorSearchStamp],
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
    registeredStamp: Option[ActorSearchStamp],
    updatedStamp: Option[ActorSearchStamp]
) extends Searchable {
  // it's safe to use get, all document does have an id.
  override val docId       = objectId.map(_.underlying.toString).get
  override val docParentId = Some(originatedObjectUuid.underlying.toString)
}

object SampleObjectSearch {

  implicit val writes: Writes[SampleObjectSearch] = Json.writes[SampleObjectSearch]

  def apply(so: SampleObject, actorNames: ActorNames): SampleObjectSearch =
    new SampleObjectSearch(
      so.objectId,
      so.originatedObjectUuid,
      so.parentObject,
      so.isExtracted,
      so.museumId,
      so.status,
      so.responsible.map(id => ActorSearch(id, actorNames.nameFor(id))),
      so.doneByStamp.map(ActorSearchStamp(_, actorNames)),
      so.sampleNum,
      so.sampleId,
      so.externalId,
      so.sampleTypeId,
      so.size,
      so.container,
      so.storageMedium,
      so.treatment,
      so.leftoverSample,
      so.description,
      so.note,
      so.registeredStamp.map(ActorSearchStamp(_, actorNames)),
      so.updatedStamp.map(ActorSearchStamp(_, actorNames))
    )

}
