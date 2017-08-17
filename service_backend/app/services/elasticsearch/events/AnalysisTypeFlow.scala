package services.elasticsearch.events

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch._
import no.uio.musit.models.ActorId
import services.actor.ActorService
import services.elasticsearch.shared.ActorEnrichFlow
import services.elasticsearch.TypeFlow

import scala.concurrent.ExecutionContext

class AnalysisTypeFlow(
    actorService: ActorService
)(implicit ec: ExecutionContext)
    extends TypeFlow[AnalysisSearchType, AnalysisSearch] {
  val withActorNames: Flow[AnalysisSearchType, AnalysisSearch, NotUsed] =
    new ActorEnrichFlow[AnalysisSearchType, AnalysisSearch] {
      override def extractActorsId(a: AnalysisSearchType): Set[ActorId] =
        Set(
          a.event.completedBy,
          a.event.doneBy,
          a.event.administrator,
          a.event.responsible,
          a.event.registeredBy,
          a.event.updatedBy,
          a.event.result.flatMap(_.registeredBy)
        ).flatten

      override def mergeWithActors(
          a: AnalysisSearchType,
          actors: Set[(ActorId, String)]
      ): AnalysisSearch =
        AnalysisSearch(a.event, ActorNames(actors))

    }.flow(actorService, ec)

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[AnalysisSearch].map { event =>
      val action = indexInto(indexConfig.indexName, event.documentType) id event.documentId doc event
      event.partOf.map(action parent _.underlying.toString).getOrElse(action)
    }

  override def populateWithData(indexConfig: IndexConfig) =
    Flow[AnalysisSearchType].via(withActorNames)

}
