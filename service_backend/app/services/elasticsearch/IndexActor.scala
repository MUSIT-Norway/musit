package services.elasticsearch

import akka.actor.{Actor, Props}
import akka.stream.ActorMaterializer
import services.elasticsearch.DocumentIndexer._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

class IndexActor(indexer: DocumentIndexer)(implicit mat: ActorMaterializer)
    extends Actor {
  import IndexActor._

  private val duration = 10 seconds

  private implicit val ec: ExecutionContext = context.dispatcher

  override def preStart(): Unit = {
    indexer.initIndex().foreach { hasIndex =>
      context.become(ready)
      if (hasIndex)
        self ! RequestLiveIndex
      else
        self ! RequestReindex
    }
  }

  def init: Receive = {
    case _: IndexActorCommand =>
      sender() ! Initialising
    case msg => unhandled(msg)
  }

  def ready: Receive = {
    case RequestLiveIndex =>
      if (indexer.updateIndexStatus().ready && indexer.reindexStatus().ready) {
        indexer.updateIndex()
        context.system.scheduler.scheduleOnce(duration, self, Status)
        sender() ! Accepted
      } else {
        sender() ! NotAccepted
      }

    case RequestReindex =>
      if (indexer.reindexStatus().ready) {
        indexer.reindex()
        context.system.scheduler.scheduleOnce(duration, self, Status)
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
  }

  override def receive: Receive = init

}

object IndexActor {
  def apply(documentIndexer: DocumentIndexer)(implicit mat: ActorMaterializer) =
    Props.apply(classOf[IndexActor], documentIndexer, mat)

  sealed trait IndexActorCommand

  case object RequestLiveIndex extends IndexActorCommand
  case object RequestReindex   extends IndexActorCommand
  case object Status           extends IndexActorCommand

  sealed trait IndexActorStatus

  case object Initialising extends IndexActorStatus
  case object Indexing     extends IndexActorStatus
  case object Ready        extends IndexActorStatus
  case object Failed       extends IndexActorStatus
  case object Accepted     extends IndexActorStatus
  case object NotAccepted  extends IndexActorStatus
}
