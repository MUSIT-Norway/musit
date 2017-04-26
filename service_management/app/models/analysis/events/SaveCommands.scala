package models.analysis.events

import models.analysis.{ActorName, ActorStamp}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectUUID
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}

object SaveCommands {

  sealed trait AnalysisEventCommand {
    type A <: AnalysisEvent

    def asDomain(implicit currUser: AuthenticatedUser): AnalysisEvent

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
      doneBy: Option[ActorName],
      doneDate: Option[DateTime],
      note: Option[String],
      objectId: ObjectUUID,
      // TODO: Add field for status
      responsible: Option[ActorName],
      administrator: Option[ActorName],
      completedBy: Option[ActorName],
      completedDate: Option[DateTime]
  ) extends SaveAnalysisEventCommand {

    override type A = Analysis

    override def asDomain(implicit currUser: AuthenticatedUser): Analysis = {
      Analysis(
        id = None,
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        registeredBy = Some(currUser.id),
        registeredDate = Some(dateTimeNow),
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
      caseNumbers: Option[Seq[String]] = None,
      cancelledReason: Option[String]
  )

  object SaveRestriction {
    implicit val reads: Reads[SaveRestriction] = Json.reads[SaveRestriction]
  }

  case class SaveAnalysisCollection(
      analysisTypeId: AnalysisTypeId,
      doneBy: Option[ActorName],
      doneDate: Option[DateTime],
      note: Option[String],
      responsible: Option[ActorName],
      administrator: Option[ActorName],
      completedBy: Option[ActorName],
      completedDate: Option[DateTime],
      // TODO: Add field for status
      objectIds: Seq[ObjectUUID],
      restriction: Option[SaveRestriction]
  ) extends SaveAnalysisEventCommand {

    override type A = AnalysisCollection

    override def asDomain(implicit currUser: AuthenticatedUser): AnalysisCollection = {
      val now = dateTimeNow
      AnalysisCollection(
        id = None,
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        registeredBy = Some(currUser.id),
        registeredDate = Some(now),
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
              registeredStamp = Some(ActorStamp(currUser.id, now))
          )
        ),
        events = this.objectIds.map { oid =>
          Analysis(
            id = None,
            analysisTypeId = analysisTypeId,
            doneBy = doneBy,
            doneDate = doneDate,
            registeredBy = Some(currUser.id),
            registeredDate = Some(now),
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
      val now = dateTimeNow
      a.copy(
        analysisTypeId = analysisTypeId,
        doneBy = doneBy,
        doneDate = doneDate,
        responsible = responsible,
        administrator = administrator,
        updatedBy = Some(cu.id),
        updatedDate = Some(now),
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
                  caseNumbers = r.caseNumbers,
                  cancelledReason = r.cancelledReason,
                  cancelledStamp = r.cancelledReason.map(_ => ActorStamp(cu.id, now))
                )
              )
              .getOrElse(
                Restriction(
                  requester = r.requester,
                  expirationDate = r.expirationDate,
                  reason = r.reason,
                  caseNumbers = r.caseNumbers,
                  registeredStamp = Some(ActorStamp(cu.id, dateTimeNow)),
                  cancelledReason = r.cancelledReason,
                  cancelledStamp = r.cancelledReason.map(_ => ActorStamp(cu.id, now))
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
