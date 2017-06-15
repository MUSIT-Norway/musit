package utils.testdata

import models.analysis.events.AnalysisResults.{AgeResult, AnalysisResult, GenericResult}
import models.analysis.events.SaveCommands.{
  ObjectUuidAndType,
  SaveAnalysis,
  SaveAnalysisCollection
}
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisTypeId, Restriction}
import models.analysis.AnalysisStatuses
import no.uio.musit.models.{ActorId, Museums, ObjectUUID, OrgId}
import no.uio.musit.models.ObjectTypes.CollectionObjectType
import no.uio.musit.time.dateTimeNow

trait AnalysisGenerators {

  protected val defaultMid          = Museums.Test.id
  protected val dummyActorId        = ActorId.generate()
  protected val dummyActorById      = ActorId.generate()
  protected val dummyAnalysisTypeId = AnalysisTypeId(1L)

  protected val dummyOrgId = OrgId(315)

  protected val oid1 = ObjectUUID.generate()
  protected val oid2 = ObjectUUID.generate()
  protected val oid3 = ObjectUUID.generate()

  protected val dummyAnalysisNote = "This is from a SaveAnalysis command"
  protected val dummyAnalysisCollectionNote =
    "This is from a SaveAnalysisCollection command"

  def dummySaveAnalysisCmd(
      oid: ObjectUUID = oid1,
      res: Option[AnalysisResult] = None
  ): SaveAnalysis = {
    SaveAnalysis(
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = Some(dateTimeNow),
      note = Some(dummyAnalysisNote),
      objectId = oid,
      responsible = Some(dummyActorById),
      administrator = Some(dummyActorById),
      completedBy = None,
      completedDate = None,
      extraAttributes = None,
      objectType = CollectionObjectType
    )
  }

  def dummySaveAnalysisCollectionCmd(
      oids: Seq[ObjectUUID] = Seq(ObjectUUID.generate()),
      res: Option[AnalysisResult] = None
  ): SaveAnalysisCollection = {
    SaveAnalysisCollection(
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = Some(dateTimeNow),
      note = Some(dummyAnalysisCollectionNote),
      responsible = Some(dummyActorById),
      administrator = Some(dummyActorById),
      completedBy = None,
      completedDate = None,
      objects = oids.map(id => ObjectUuidAndType(id, CollectionObjectType)),
      restriction = None,
      reason = None,
      status = AnalysisStatuses.Preparation,
      caseNumbers = None,
      extraAttributes = None,
      orgId = Some(dummyOrgId)
    )
  }

  def dummyGenericResult(
      extRef: Option[Seq[String]] = None,
      comment: Option[String] = None
  ): GenericResult = {
    GenericResult(
      registeredBy = Some(dummyActorId),
      registeredDate = Some(dateTimeNow),
      extRef = extRef,
      comment = comment
    )
  }

  def dummyDatingResult(
      extRef: Option[Seq[String]] = None,
      comment: Option[String] = None,
      age: Option[String] = None
  ): AgeResult = {
    AgeResult(
      registeredBy = Some(dummyActorId),
      registeredDate = Some(dateTimeNow),
      extRef = None,
      comment = None,
      age = age
    )
  }

  def dummyAnalysis(
      oid: Option[ObjectUUID],
      res: Option[AnalysisResult] = None
  ): Analysis = {
    val now = Some(dateTimeNow)
    Analysis(
      id = None,
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = now,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      responsible = Some(dummyActorById),
      administrator = Some(dummyActorById),
      updatedBy = Some(dummyActorId),
      updatedDate = now,
      completedBy = Some(dummyActorById),
      completedDate = now,
      partOf = None,
      objectId = oid,
      objectType = oid.map(_ => CollectionObjectType),
      note = Some("This is the first event"),
      extraAttributes = None,
      result = res
    )
  }

  def dummyAnalysisCollection(
      res: Option[AnalysisResult],
      analyses: Analysis*
  ): AnalysisCollection = {
    val now = Some(dateTimeNow)
    AnalysisCollection(
      id = None,
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = now,
      registeredBy = Some(dummyActorId),
      registeredDate = now,
      responsible = Some(dummyActorById),
      administrator = Some(dummyActorById),
      updatedBy = None,
      updatedDate = None,
      completedBy = Some(dummyActorById),
      completedDate = now,
      note = Some("An analysis collection"),
      extraAttributes = None,
      result = res,
      events = analyses.toSeq,
      restriction = Some(Restriction(ActorId.generate(), dateTimeNow, "some reason")),
      reason = None,
      status = None,
      caseNumbers = None,
      orgId = Some(dummyOrgId)
    )
  }

}
