package services.elasticsearch.index

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitGeneralError, MusitResult, MusitSuccess}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import services.elasticsearch.index.IndexProcessor.Protocol._
import services.elasticsearch.index.analysis.IndexAnalysis
import services.elasticsearch.index.objects.IndexObjects
import services.elasticsearch.index.conservation.IndexConservation

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Service wrapper around managing indexing so we're not exposing the actor ref.
 * This is to encapsulate the indexing from the rest of the application. It will
 * also run on it's own actor system to prevent performance issues and isolate it from
 * the rest of the application.
 */
@Singleton
class ElasticsearchIndexService @Inject()(
    indexAnalysis: IndexAnalysis,
    indexMusitObjects: IndexObjects,
    indexConservation: IndexConservation,
    indexMaintainer: IndexMaintainer,
    lifecycle: ApplicationLifecycle,
    configuration: Configuration
) {

  /**
   * Implicits needed by the IndexProcessor and Indexer trait.
   */
  private implicit val as = ActorSystem(
    "musit-elasticsearch",
    configuration.getOptional[Configuration]("musit.elasticsearch").map(_.underlying)
  )
  private implicit val mat = ActorMaterializer()
  private implicit val ec  = as.dispatcher
  private implicit val to  = Timeout(10 seconds)

  private val updateInterval = Some(2 minute)
  private val analysisActor = as.actorOf(
    IndexProcessor(indexAnalysis, indexMaintainer, updateInterval),
    "IndexProcessor-Analysis"
  )
  private val objectsActor = as.actorOf(
    IndexProcessor(indexMusitObjects, indexMaintainer, updateInterval),
    "IndexProcessor-Objects"
  )

  private val conservationActor = as.actorOf(
    IndexProcessor(indexConservation, indexMaintainer, updateInterval),
    "IndexProcessor-Conservation"
  )

  val actors = Array(analysisActor, objectsActor, conservationActor)

  lifecycle.addStopHook { () =>
    mat.shutdown()
    as.terminate()
  }

  def reIndex(actor: ActorRef): Future[MusitResult[Unit]] =
    sendToActor(actor, RequestReindex)

  def updateIndex(actor: ActorRef): Future[MusitResult[Unit]] =
    sendToActor(actor, RequestUpdateIndex)

  def reindexAll(): Future[Seq[MusitResult[Unit]]] =
    Future.sequence(actors.map(actor => reIndex(actor)))

  def updateAllIndices(): Future[Seq[MusitResult[Unit]]] =
    Future.sequence(actors.map(actor => updateIndex(actor)))

  private def sendToActor(actor: ActorRef, cmd: IndexActorCommand) =
    (actor ? cmd).map {
      case Accepted => MusitSuccess(())
      case other: IndexActorStatus => {
        //TODO: Log this?
        MusitGeneralError(other.toString)
      }
    }
}
