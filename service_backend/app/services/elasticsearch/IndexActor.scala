package services.elasticsearch

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.stream.ActorMaterializer
import services.elasticsearch.DocumentIndexer._
import services.elasticsearch.IndexActor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class IndexActor(indexer: DocumentIndexer, updateInterval: FiniteDuration)(
    implicit mat: ActorMaterializer
) extends Actor {

  private implicit val ec: ExecutionContext = context.dispatcher
  private implicit val as: ActorSystem      = context.system

  private var updateIndexScheduler: Option[Cancellable] = None

  override def preStart(): Unit = {
    indexer.initIndex().foreach { hasIndex =>
      context.become(ready)
      if (hasIndex)
        self ! RequestUpdateIndex
      else
        self ! RequestReindex
    }
    updateIndexScheduler = Some(
      context.system.scheduler
        .schedule(updateInterval, updateInterval, self, RequestUpdateIndex)
    )
  }

  override def postStop(): Unit =
    updateIndexScheduler.foreach(_.cancel())

  def init: Receive = {
    case _: IndexActorCommand =>
      sender() ! Initialising
    case msg => unhandled(msg)
  }

  def ready: Receive = {
    case RequestUpdateIndex =>
      if (indexer.updateIndexStatus().ready && indexer.reindexStatus().ready) {
        indexer.updateIndex()
        sender() ! Accepted
      } else {
        sender() ! NotAccepted
      }

    case RequestReindex =>
      if (indexer.reindexStatus().ready) {
        indexer.reindex()
        sender() ! Accepted
      } else {
        sender() ! NotAccepted
      }

    case Status =>
      if (indexer.reindexStatus() == Executing || indexer
            .updateIndexStatus() == Executing) {
        sender() ! Indexing
      } else if (indexer.updateIndexStatus().ready && indexer.reindexStatus().ready) {
        sender() ! Ready
      } else {
        sender() ! Failed
      }

    case msg =>
      unhandled(msg)
  }

  override def receive: Receive = init

}

object IndexActor {
  def apply(documentIndexer: DocumentIndexer)(implicit mat: ActorMaterializer) =
    Props.apply(classOf[IndexActor], documentIndexer, 1 minute, mat)

  /**
   * Commands that can be sent to the actor
   */
  sealed trait IndexActorCommand
  case object RequestUpdateIndex extends IndexActorCommand
  case object RequestReindex     extends IndexActorCommand
  case object Status             extends IndexActorCommand

  /**
   * Responses on commands sent to the actor
   */
  sealed trait IndexActorStatus
  case object Initialising extends IndexActorStatus
  case object Indexing     extends IndexActorStatus
  case object Ready        extends IndexActorStatus
  case object Failed       extends IndexActorStatus
  case object Accepted     extends IndexActorStatus
  case object NotAccepted  extends IndexActorStatus

}
