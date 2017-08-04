package repositories.elasticsearch.dao

import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import no.uio.musit.models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import repositories.analysis.dao.AnalysisEventTableProvider
import slick.basic.DatabasePublisher

@Singleton
class ElasticsearchEventDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends AnalysisEventTableProvider {

  import profile.api._

  def analysisEventsStream[E >: ExportEventRow](): DatabasePublisher[E] = {
    val query = eventTable.joinLeft(resultTable).on(_.eventId === _.eventId)
    db.stream(query.result).mapResult(res => toAnalysisEvent(res))
  }

  def toAnalysisEvent(res: (EventRow, Option[ResultRow])) = {
    val event = Json
      .fromJson[AnalysisModuleEvent](res._1._13)
      .map {
        case a: Analysis =>
          a.copy(id = res._1._1)
            .withResult(res._2.map(r => Json.fromJson[AnalysisResult](r._5).get))
        case ac: AnalysisCollection =>
          ac.copy(id = res._1._1)
            .withResult(res._2.map(r => Json.fromJson[AnalysisResult](r._5).get))
        case sc: SampleCreated => sc.copy(id = res._1._1)
      }
      .get
    AnalysisEventRow(res._1._3, event)
  }

}

sealed trait ExportEventRow

case class AnalysisEventRow(
    museumId: MuseumId,
    event: AnalysisModuleEvent
) extends ExportEventRow
