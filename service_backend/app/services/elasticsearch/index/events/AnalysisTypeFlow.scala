package services.elasticsearch.index.events

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch._
import no.uio.musit.models.{ActorId, MuseumCollections, MuseumId, ObjectUUID}
import repositories.elasticsearch.dao.ElasticsearchThingsDao
import services.actor.ActorService
import services.elasticsearch.index.TypeFlow
import services.elasticsearch.index.shared.{
  ActorEnrichFlow,
  MuseumAndCollectionEnrichFlow
}

import scala.concurrent.ExecutionContext

class AnalysisTypeFlow(
    actorService: ActorService,
    elasticsearchThingsDao: ElasticsearchThingsDao
)(implicit ec: ExecutionContext)
    extends TypeFlow[AnalysisSearchType, AnalysisSearch] {

  private val withActorNames =
    new ActorEnrichFlow[AnalysisSearchType, (AnalysisSearchType, ActorNames)] {
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
      ): (AnalysisSearchType, ActorNames) = (a, ActorNames(actors))

    }.flow(actorService, ec)

  private val withOriginatedObject =
    new MuseumAndCollectionEnrichFlow[(AnalysisSearchType, ActorNames), AnalysisSearch] {
      override def extractObjectUUID(
          input: (AnalysisSearchType, ActorNames)
      ): Option[ObjectUUID] =
        input._1.objectUuid

      override def mergeToOutput(
          input: (AnalysisSearchType, ActorNames),
          midAndColl: Option[(MuseumId, MuseumCollections.Collection)]
      ): AnalysisSearch = {
        AnalysisSearch(input._1.event, input._2, midAndColl)
      }
    }.flow(elasticsearchThingsDao, ec)

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[AnalysisSearch].map { event =>
      val action = indexInto(indexConfig.name, analysisType) id event.docId doc event
      event.partOf.map(action parent _.underlying.toString).getOrElse(action)
    }

  override def populateWithData(indexConfig: IndexConfig) =
    Flow[AnalysisSearchType].via(withActorNames).via(withOriginatedObject)

}
