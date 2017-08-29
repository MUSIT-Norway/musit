package services.elasticsearch.index.events

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch._
import no.uio.musit.models.ActorId
import services.actor.ActorService
import services.elasticsearch.index.TypeFlow
import services.elasticsearch.index.shared.ActorEnrichFlow

import scala.concurrent.ExecutionContext

class SampleCreatedTypeFlow(
    actorService: ActorService
)(implicit ec: ExecutionContext)
    extends TypeFlow[SampleCreatedEventSearchType, SampleCreatedSearch] {

  val withActorNames: Flow[SampleCreatedEventSearchType, SampleCreatedSearch, NotUsed] =
    new ActorEnrichFlow[SampleCreatedEventSearchType, SampleCreatedSearch] {
      override def extractActorsId(a: SampleCreatedEventSearchType): Set[ActorId] =
        Set(a.event.doneBy, a.event.registeredBy).flatten

      override def mergeWithActors(
          a: SampleCreatedEventSearchType,
          actors: Set[(ActorId, String)]
      ): SampleCreatedSearch =
        SampleCreatedSearch(a.event, ActorNames(actors))

    }.flow(actorService, ec)

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[SampleCreatedSearch].map { event =>
      indexInto(indexConfig.name, sampleType) id event.docId doc event
    }

  override def populateWithData(indexConfig: IndexConfig) =
    Flow[SampleCreatedEventSearchType].via(withActorNames)

}
