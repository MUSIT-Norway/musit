package models.analysis.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectUUID
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}

object SaveCommands {

  sealed trait SaveAnalysisEventCommand {
    def asDomain: AnalysisEvent
  }

  case class SaveAnalysis(
      analysisTypeId: AnalysisTypeId,
      eventDate: Option[DateTime],
      note: Option[String],
      objectId: ObjectUUID
  ) extends SaveAnalysisEventCommand {

    override def asDomain: Analysis = {
      Analysis(
        id = None,
        analysisTypeId = analysisTypeId,
        eventDate = eventDate,
        registeredBy = None,
        registeredDate = None,
        objectId = Some(objectId),
        partOf = None,
        note = note,
        result = None
      )
    }

  }

  object SaveAnalysis extends WithDateTimeFormatters {

    implicit val reads: Reads[SaveAnalysis] = Json.reads[SaveAnalysis]

  }

  case class SaveAnalysisCollection(
      analysisTypeId: AnalysisTypeId,
      eventDate: Option[DateTime],
      note: Option[String],
      objectIds: Seq[ObjectUUID]
  ) extends SaveAnalysisEventCommand {

    override def asDomain: AnalysisCollection = {
      AnalysisCollection(
        id = None,
        analysisTypeId = this.analysisTypeId,
        eventDate = this.eventDate,
        registeredBy = None,
        registeredDate = None,
        events = this.objectIds.map { oid =>
          Analysis(
            id = None,
            analysisTypeId = this.analysisTypeId,
            eventDate = this.eventDate,
            registeredBy = None,
            registeredDate = None,
            objectId = Option(oid),
            partOf = None,
            note = this.note,
            result = None
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
