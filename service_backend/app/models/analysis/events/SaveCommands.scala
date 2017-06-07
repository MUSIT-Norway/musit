package models.analysis.events

import models.analysis.ActorStamp
import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.events.AnalysisExtras.ExtraAttributes
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, CaseNumbers, ObjectUUID}
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
      doneBy: Option[ActorId],
      doneDate: Option[DateTime],
      note: Option[String],
      objectId: ObjectUUID,
      objectType: ObjectType,
      extraAttributes: Option[ExtraAttributes],
      responsible: Option[ActorId],
      administrator: Option[ActorId],
      completedBy: Option[ActorId],
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
        objectType = Some(objectType),
        responsible = responsible,
        administrator = administrator,
        updatedBy = None,
        updatedDate = None,
        completedBy = completedBy,
        completedDate = completedDate,
        partOf = None,
        note = note,
        extraAttributes = extraAttributes,
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
      requester: ActorId,
      expirationDate: DateTime,
      reason: String,
      caseNumbers: Option[CaseNumbers] = None,
      cancelledReason: Option[String]
  )

  object SaveRestriction {
    implicit val reads: Reads[SaveRestriction] = Json.reads[SaveRestriction]
  }

  case class ObjectUuidAndType(objectId: ObjectUUID, objectType: ObjectType)

  object ObjectUuidAndType {
    implicit val reads: Reads[ObjectUuidAndType] = Json.reads[ObjectUuidAndType]
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
      objects: Seq[ObjectUuidAndType],
      restriction: Option[SaveRestriction],
      caseNumbers: Option[CaseNumbers],
      reason: Option[String],
      status: AnalysisStatus,
      extraAttributes: Option[ExtraAttributes]
  ) extends SaveAnalysisEventCommand {

    override type A = AnalysisCollection

    // scalastyle:off method.length
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
        extraAttributes = extraAttributes,
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
        reason = reason,
        status = Option(status),
        caseNumbers = caseNumbers,
        events = this.objects.map { oid =>
          Analysis(
            id = None,
            analysisTypeId = analysisTypeId,
            doneBy = doneBy,
            doneDate = doneDate,
            registeredBy = Some(currUser.id),
            registeredDate = Some(now),
            objectId = Option(oid.objectId),
            objectType = Option(oid.objectType),
            responsible = responsible,
            administrator = administrator,
            updatedBy = None,
            updatedDate = None,
            completedBy = completedBy,
            completedDate = completedDate,
            partOf = None,
            note = this.note,
            extraAttributes = None,
            result = None
          )
        }
      )
    }
    // scalastyle:on method.length

    override def updateDomain(
        a: AnalysisCollection
    )(implicit cu: AuthenticatedUser): AnalysisCollection = {
      val now = dateTimeNow
      a.copy(
        doneBy = doneBy,
        doneDate = doneDate,
        responsible = responsible,
        administrator = administrator,
        updatedBy = Some(cu.id),
        updatedDate = Some(now),
        completedBy = completedBy,
        completedDate = completedDate,
        reason = reason,
        status = Option(status),
        caseNumbers = caseNumbers,
        note = note,
        restriction = restriction.map { r =>
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
        }
      )
    }
  }

  object SaveAnalysisCollection extends WithDateTimeFormatters {

    implicit val reads: Reads[SaveAnalysisCollection] =
      Json.reads[SaveAnalysisCollection]

  }

}
