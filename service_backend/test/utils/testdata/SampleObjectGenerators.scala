package utils.testdata

import models.analysis.LeftoverSamples.NoLeftover
import models.analysis._
import models.analysis.events.SampleCreated
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import org.joda.time.DateTime

trait SampleObjectGenerators {

  val mid            = Museums.Test.id
  val defaultActorId = ActorId.generate()

  def generateSample(
      id: ObjectUUID,
      parentId: Option[ObjectUUID],
      parentObjType: ObjectType,
      origObjectId: ObjectUUID = ObjectUUID.generate(),
      isExtracted: Boolean = true
  ): SampleObject = {
    val now = DateTime.now
    SampleObject(
      objectId = Some(id),
      parentObject = ParentObject(parentId, parentObjType),
      isExtracted = isExtracted,
      museumId = mid,
      status = SampleStatuses.Intact,
      responsible = ActorId.generateAsOpt(),
      doneByStamp = ActorId.generateAsOpt().map(ActorStamp(_, now)),
      sampleId = None,
      sampleNum = None,
      externalId = Some(ExternalId("external id", Some("external source"))),
      sampleTypeId = SampleTypeId(1),
      size = Some(Size("cm2", 12.0)),
      container = Some("box"),
      storageMedium = None,
      treatment = Some("treatment"),
      leftoverSample = NoLeftover,
      description = Some("sample description"),
      note = Some("This is a sample note"),
      originatedObjectUuid = origObjectId,
      registeredStamp = Some(ActorStamp(ActorId.generate(), now)),
      updatedStamp = None,
      isDeleted = false
    )
  }

  def generateSampleEvent(
      doneBy: Option[ActorId] = None,
      doneDate: Option[DateTime] = None,
      registeredBy: Option[ActorId] = Some(defaultActorId),
      registeredDate: Option[DateTime] = None,
      objectId: Option[ObjectUUID] = ObjectUUID.generateAsOpt(),
      sampleObjectId: Option[ObjectUUID] = ObjectUUID.generateAsOpt()
  ): SampleCreated = {
    val now = DateTime.now
    SampleCreated(
      id = None,
      doneBy = doneBy,
      doneDate = doneDate,
      registeredBy = registeredBy,
      registeredDate = registeredDate,
      objectId = objectId,
      sampleObjectId = sampleObjectId,
      externalLinks = None
    )
  }

}
