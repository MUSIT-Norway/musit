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
import services.elasticsearch.index.events.IndexEvents
import services.elasticsearch.index.things.IndexObjects

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Service wrapper around managing indexing so we're not exposing the actor ref.
 * This is to encapsulate the indexing from the rest of the application. It will
 * also run on it's own actor system to prevent performance issues and isolate it from
 * the rest of the application.
 */
@Singleton
class ElasticsearchService @Inject(
  )(
    indexAnalysisEvents: IndexEvents,
    indexMusitObjects: IndexObjects,
    indexMaintainer: IndexMaintainer,
    lifecycle: ApplicationLifecycle,
    configuration: Configuration
) {

  /**
   * Implicits needed by the IndexProcessor and Indexer trait.
   */
  private implicit val as = ActorSystem(
    "musit-elasticsearch",
    configuration.getOptional[Configuration]("musit.elasticsearch.akka").map(_.underlying)
  )
  private implicit val mat = ActorMaterializer()
  private implicit val ec  = as.dispatcher
  private implicit val to  = Timeout(10 seconds)

  private val updateInterval = Some(5 minute)
  private val eventActor = as.actorOf(
    IndexProcessor(indexAnalysisEvents, indexMaintainer, updateInterval)
  )
  private val thingsActor = as.actorOf(
    IndexProcessor(indexMusitObjects, indexMaintainer, updateInterval)
  )

  lifecycle.addStopHook { () =>
    mat.shutdown()
    as.terminate()
  }

  def reIndexEvents(): Future[MusitResult[Unit]] =
    sendToActor(eventActor, RequestReindex)

  def updateIndexEvents(): Future[MusitResult[Unit]] =
    sendToActor(eventActor, RequestUpdateIndex)

  def reindexThings(): Future[MusitResult[Unit]] =
    sendToActor(thingsActor, RequestReindex)

  def updateIndexThings(): Future[MusitResult[Unit]] =
    sendToActor(thingsActor, RequestUpdateIndex)

  def reindexAll(): Future[Seq[MusitResult[Unit]]] =
    Future.sequence(List(reIndexEvents(), reindexThings()))

  def updateAllIndices(): Future[Seq[MusitResult[Unit]]] =
    Future.sequence(List(updateIndexEvents(), updateIndexThings()))

  private def sendToActor(actor: ActorRef, cmd: IndexActorCommand) =
    (actor ? cmd).map {
      case Accepted                => MusitSuccess(())
      case other: IndexActorStatus => MusitGeneralError(other.toString)
    }

}
