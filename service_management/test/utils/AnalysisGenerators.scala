package utils

import models.analysis.events.AnalysisResults.{
  AnalysisResult,
  DatingResult,
  GenericResult
}
import models.analysis.events.SaveCommands.{SaveAnalysis, SaveAnalysisCollection}
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisTypeId, Restriction}
import no.uio.musit.models.{ActorId, Museums, ObjectUUID}
import no.uio.musit.time.dateTimeNow

trait AnalysisGenerators {

  protected val defaultMid          = Museums.Test.id
  protected val dummyActorId        = ActorId.generate()
  protected val dummyAnalysisTypeId = AnalysisTypeId.generate()

  protected val oid1 = ObjectUUID.generate()
  protected val oid2 = ObjectUUID.generate()
  protected val oid3 = ObjectUUID.generate()

  def dummySaveAnalysisCmd(
      oid: ObjectUUID = oid1,
      res: Option[AnalysisResult] = None
  ): SaveAnalysis = {
    SaveAnalysis(
      analysisTypeId = dummyAnalysisTypeId,
      doneBy = Some(dummyActorId),
      doneDate = Some(dateTimeNow),
      note = Some("This is from a SaveAnalysis command"),
      objectId = oid,
      responsible = Some(dummyActorId),
      administrator = Some(dummyActorId),
      completedBy = None,
      completedDate = None
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
      note = Some("This is from a SaveAnalysisCollection command"),
      responsible = Some(dummyActorId),
      administrator = Some(dummyActorId),
      completedBy = None,
      completedDate = None,
      objectIds = oids,
      restriction = None
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
  ): DatingResult = {
    DatingResult(
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
      responsible = Some(dummyActorId),
      administrator = Some(dummyActorId),
      updatedBy = Some(dummyActorId),
      updatedDate = now,
      completedBy = Some(dummyActorId),
      completedDate = now,
      partOf = None,
      objectId = oid,
      note = Some("This is the first event"),
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
      responsible = Some(dummyActorId),
      administrator = Some(dummyActorId),
      updatedBy = None,
      updatedDate = None,
      completedBy = Some(dummyActorId),
      completedDate = now,
      note = Some("An analysis collection"),
      result = res,
      events = analyses.toSeq,
      restriction = Some(Restriction("requester", dateTimeNow, "some reason"))
    )
  }

}
