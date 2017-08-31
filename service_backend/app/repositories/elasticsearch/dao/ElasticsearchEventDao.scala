package repositories.elasticsearch.dao

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import models.elasticsearch.AnalysisModuleEventSearch
import no.uio.musit.models.{ObjectId, ObjectUUID}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import repositories.analysis.dao.{AnalysisEventTableProvider, AnalysisTables}

@Singleton
class ElasticsearchEventDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends AnalysisEventTableProvider
    with AnalysisTables {

  import profile.api._

  def analysisEventsStream[E >: AnalysisModuleEventSearch](
      eventsAfter: Option[DateTime] = None
  ): Source[E, NotUsed] = {
    val baseQuery2 = eventTable.joinLeft(resultTable).on(_.eventId === _.eventId)

    val baseQuery = for {
      ((evt, res), sam) <- eventTable
                            .joinLeft(resultTable)
                            .on(_.eventId === _.eventId)
                            .joinLeft(sampleObjTable)
                            .on(_._1.affectedUuid.asColumnOf[ObjectUUID] === _.id)
    } yield (evt, res, sam)

    val query = eventsAfter.map { date =>
      baseQuery.filter {
        case (evt, _, _) => evt.updatedDate > date || evt.registeredDate > date
      }.map { case (evt, res, sam) => (evt, res, sam) }
    }.getOrElse(baseQuery)

    Source.fromPublisher(db.stream(query.result).mapResult(toAnalysisEvent))
  }

  private def toAnalysisEvent(
      res: (EventRow, Option[ResultRow], Option[SampleObjectRow])
  ) = {
    val event = Json
      .fromJson[AnalysisModuleEvent](res._1._14)
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

    val objUuid = res._3.map(fromSampleObjectRow).map(_.originatedObjectUuid)
    AnalysisModuleEventSearch(res._1._3, objUuid, event)
  }

}
