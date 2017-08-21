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

class AnalysisCollectionTypeFlow(
    actorService: ActorService
)(implicit ec: ExecutionContext)
    extends TypeFlow[AnalysisCollectionSearchType, AnalysisCollectionSearch] {

  val withActorNames
    : Flow[AnalysisCollectionSearchType, AnalysisCollectionSearch, NotUsed] =
    new ActorEnrichFlow[AnalysisCollectionSearchType, AnalysisCollectionSearch] {
      override def extractActorsId(a: AnalysisCollectionSearchType): Set[ActorId] =
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
          a: AnalysisCollectionSearchType,
          actors: Set[(ActorId, String)]
      ): AnalysisCollectionSearch =
        AnalysisCollectionSearch(a.event, ActorNames(actors))

    }.flow(actorService, ec)

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[AnalysisCollectionSearch].map { event =>
      indexInto(indexConfig.name, analysisCollectionType) id event.docId doc event
    }

  override def populateWithData(indexConfig: IndexConfig) =
    Flow[AnalysisCollectionSearchType].via(withActorNames)

}
