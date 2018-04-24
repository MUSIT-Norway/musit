package services.elasticsearch.index.conservation

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.conservation.events.{ConservationType, EventRole}
import models.elasticsearch._
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.ActorId
import play.api.Logger
import services.actor.ActorService
import services.elasticsearch.index.shared.ActorEnrichFlow

import scala.concurrent.ExecutionContext

/**
 * Creates a flow which transforms the initial ConservationSearch objects into something which can be sent to ElasticSearch
 *
 * This didn't have to be a class, it is used only as a singleton and contains only on method, so it could as well have been a single function
 *
 * The word 'Type' in 'ConservationTypeFlow' is an ES term (soon outdated, so it's really only confusing here and I'd prefer
 * to remove it, but using it here to be consistent with analysis and object indexing)
 *
 */
class ConservationTypeFlow(
    actorService: ActorService,
    allEventRoles: Seq[EventRole],
    config: IndexConfig
)(implicit ec: ExecutionContext) {

  val logger = Logger(classOf[ConservationTypeFlow])

  val logAndRemoveErrorsFlow
    : Flow[MusitResult[DeletedOrExistingConservationSearchObject], DeletedOrExistingConservationSearchObject, NotUsed] =
    Flow[MusitResult[DeletedOrExistingConservationSearchObject]].map { element =>
      val res = element match {
        case x: MusitSuccess[DeletedOrExistingConservationSearchObject] =>
          x
        case err: MusitError =>
          logger.error(s"ES indexing error: ${err.message}")
          err
      }
      res
    }.collect {
      case mr: MusitSuccess[DeletedOrExistingConservationSearchObject] => mr.value
    }

  val withActorNamesFlow: Flow[
    DeletedOrExistingConservationSearchObject,
    DeletedOrExistingConservationSearchObject,
    NotUsed
  ] =
    new ActorEnrichFlow[
      DeletedOrExistingConservationSearchObject,
      DeletedOrExistingConservationSearchObject
    ] {
      override def extractActorsId(
          a: DeletedOrExistingConservationSearchObject
      ): Set[ActorId] = {
        a.collectMentionedActorIds()
      }

      override def mergeWithActors(
          a: DeletedOrExistingConservationSearchObject,
          actors: Set[(ActorId, String)]
      ): DeletedOrExistingConservationSearchObject =
        a.withActorNames(ActorNames(actors), allEventRoles)

    }.flow(actorService, ec)

  //This one will eventually flow into ElasticSearch
  val intoEsFlow
    : Flow[DeletedOrExistingConservationSearchObject, BulkCompatibleDefinition, NotUsed] =
    Flow[DeletedOrExistingConservationSearchObject].map { thing =>
      val res =
        indexInto(config.name, conservationType).id(thing.eventId).doc(thing.toJson())
      res
    }

  ///Creates a flow which logs and removes errors and puts the successful events into ES
  def createFlow(
      in: Source[MusitResult[DeletedOrExistingConservationSearchObject], NotUsed]
  ) /*: Flow[MusitResult[ConservationEvent], BulkCompatibleDefinition, NotUsed]*/ = {

    in.via(logAndRemoveErrorsFlow).via(withActorNamesFlow).via(intoEsFlow)
  }

}
