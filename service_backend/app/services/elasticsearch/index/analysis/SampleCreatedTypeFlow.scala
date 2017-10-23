package services.elasticsearch.index.analysis

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch._
import no.uio.musit.models.{ActorId, MuseumCollections, MuseumId}
import repositories.elasticsearch.dao.ElasticsearchObjectsDao
import services.actor.ActorService
import services.elasticsearch.index.TypeFlow
import services.elasticsearch.index.shared.{
  ActorEnrichFlow,
  MuseumAndCollectionEnrichFlow
}

import scala.concurrent.ExecutionContext

class SampleCreatedTypeFlow(
    actorService: ActorService,
    elasticsearchObjectsDao: ElasticsearchObjectsDao
)(implicit ec: ExecutionContext)
    extends TypeFlow[SampleCreatedEventSearchType, SampleCreatedSearch] {

  val withActorNames: Flow[
    SampleCreatedEventSearchType,
    (SampleCreatedEventSearchType, ActorNames),
    NotUsed
  ] =
    new ActorEnrichFlow[
      SampleCreatedEventSearchType,
      (SampleCreatedEventSearchType, ActorNames)
    ] {
      override def extractActorsId(a: SampleCreatedEventSearchType): Set[ActorId] =
        Set(a.event.doneBy, a.event.registeredBy).flatten

      override def mergeWithActors(
          a: SampleCreatedEventSearchType,
          actors: Set[(ActorId, String)]
      ): (SampleCreatedEventSearchType, ActorNames) = (a, ActorNames(actors))

    }.flow(actorService, ec)

  val withMuseumAndCollection = new MuseumAndCollectionEnrichFlow[
    (SampleCreatedEventSearchType, ActorNames),
    SampleCreatedSearch
  ] {
    override def extractObjectUUID(input: (SampleCreatedEventSearchType, ActorNames)) =
      input._1.objectUuid

    override def mergeToOutput(
        input: (SampleCreatedEventSearchType, ActorNames),
        midAndColl: Option[(MuseumId, MuseumCollections.Collection)]
    ) = SampleCreatedSearch(input._1.event, input._2, midAndColl)
  }.flow(elasticsearchObjectsDao, ec)

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[SampleCreatedSearch].map { event =>
      indexInto(indexConfig.name, sampleType) id event.docId doc event
    }

  override def populateWithData(indexConfig: IndexConfig) =
    Flow[SampleCreatedEventSearchType].via(withActorNames).via(withMuseumAndCollection)

}
