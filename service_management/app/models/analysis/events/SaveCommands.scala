package models.analysis.events

import models.analysis.ActorStamp
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.{ActorId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}

object SaveCommands {

  sealed trait AnalysisEventCommand {
    type A <: AnalysisEvent

    def asDomain: AnalysisEvent

    def updateDomain(a: A)(implicit cu: AuthenticatedUser): A
  }

  sealed trait SaveAnalysisEventCommand extends AnalysisEventCommand

  object SaveAnalysisEventCommand {

    implicit val reads: Reads[SaveAnalysisEventCommand] = Reads { jsv =>
      jsv.validate[SaveAnalysis] orElse jsv.validate[SaveAnalysisCollection]
    }

    def updateDomain[Cmd <: SaveAnalysisEventCommand, Evt <: AnalysisEvent](
        cmd: Cmd,
        evt: Evt
    )(implicit cu: AuthenticatedUser): AnalysisEvent = {
      cmd match {
        case sa: SaveAnalysis =>
          sa.updateDomain(evt.asInstanceOf[Analysis])

        case sc: SaveAnalysisCollection =>
          sc.updateDomain(evt.asInstanceOf[AnalysisCollection])
      }
    }

  }

  case class SaveAnalysis(
      analysisTypeId: AnalysisTypeId,
      doneBy: Option[ActorId],
      doneDate: Option[DateTime],
      note: Option[String],
      objectId: ObjectUUID,
      // TODO: Add field for status
      responsible: Option[ActorId],
      administrator: Option[ActorId],
      completedBy: Option[ActorId],
      completedDate: Option[DateTime]
  ) extends SaveAnalysisEventCommand {

    override type A = Analysis

    override def asDomain: Analysis = {
      Analysis(
        id = None,
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        registeredBy = None,
        registeredDate = None,
        objectId = Some(objectId),
        responsible = responsible,
        administrator = administrator,
        updatedBy = None,
        updatedDate = None,
        completedBy = completedBy,
        completedDate = completedDate,
        partOf = None,
        note = note,
        result = None
      )
    }

    override def updateDomain(a: Analysis)(implicit cu: AuthenticatedUser): Analysis = {
      a.copy(
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        objectId = Some(objectId),
        responsible = responsible,
        administrator = administrator,
        updatedBy = Some(cu.id),
        updatedDate = Some(dateTimeNow),
        completedBy = completedBy,
        completedDate = completedDate,
        note = note
      )
    }
  }

  object SaveAnalysis extends WithDateTimeFormatters {

    implicit val reads: Reads[SaveAnalysis] = Json.reads[SaveAnalysis]

  }

  case class SaveRestriction(
      requester: String,
      expirationDate: DateTime,
      reason: String,
      caseNumbers: Option[Seq[String]] = None
  )

  object SaveRestriction {
    implicit val reads: Reads[SaveRestriction] = Json.reads[SaveRestriction]
  }

  case class SaveAnalysisCollection(
      analysisTypeId: AnalysisTypeId,
      doneBy: Option[ActorId],
      doneDate: Option[DateTime],
      note: Option[String],
      responsible: Option[ActorId],
      administrator: Option[ActorId],
      completedBy: Option[ActorId],
      completedDate: Option[DateTime],
      // TODO: Add field for status
      objectIds: Seq[ObjectUUID],
      restriction: Option[Restriction]
  ) extends SaveAnalysisEventCommand {

    override type A = AnalysisCollection

    override def asDomain: AnalysisCollection = {
      AnalysisCollection(
        id = None,
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        registeredBy = None,
        registeredDate = None,
        responsible = responsible,
        administrator = administrator,
        updatedBy = None,
        updatedDate = None,
        completedBy = completedBy,
        completedDate = completedDate,
        note = note,
        result = None,
        restriction = restriction.map(
          r =>
            Restriction(
              requester = r.requester,
              expirationDate = r.expirationDate,
              reason = r.reason,
              caseNumbers = r.caseNumbers,
              registeredStamp = None
          )
        ),
        events = this.objectIds.map { oid =>
          Analysis(
            id = None,
            analysisTypeId = analysisTypeId,
            doneBy = doneBy,
            doneDate = doneDate,
            registeredBy = None,
            registeredDate = None,
            objectId = Option(oid),
            responsible = responsible,
            administrator = administrator,
            updatedBy = None,
            updatedDate = None,
            completedBy = completedBy,
            completedDate = completedDate,
            partOf = None,
            note = this.note,
            result = None
          )
        }
      )
    }

    override def updateDomain(
        a: AnalysisCollection
    )(implicit cu: AuthenticatedUser): AnalysisCollection = {
      a.copy(
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        responsible = responsible,
        administrator = administrator,
        updatedBy = Some(cu.id),
        updatedDate = Some(dateTimeNow),
        completedBy = completedBy,
        completedDate = completedDate,
        note = note,
        restriction = restriction.map(
          r =>
            a.restriction
              .map(
                _.copy(
                  requester = r.requester,
                  expirationDate = r.expirationDate,
                  reason = r.reason,
                  caseNumbers = r.caseNumbers
                )
              )
              .getOrElse(
                Restriction(
                  requester = r.requester,
                  expirationDate = r.expirationDate,
                  reason = r.reason,
                  caseNumbers = r.caseNumbers,
                  registeredStamp = Some(ActorStamp(cu.id, dateTimeNow))
                )
            )
        )
      )
    }
  }

  object SaveAnalysisCollection extends WithDateTimeFormatters {

    implicit val reads: Reads[SaveAnalysisCollection] =
      Json.reads[SaveAnalysisCollection]

  }

}
