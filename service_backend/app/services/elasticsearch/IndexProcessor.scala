package services.elasticsearch

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.stream.ActorMaterializer
import models.elasticsearch.DocumentIndexerStatuses._
import models.elasticsearch.{IndexCallback, IndexConfig}
import services.elasticsearch.IndexProcessor.InternalProtocol._
import services.elasticsearch.IndexProcessor.Protocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Actor that keep the current status on the indexing process. It takes commands
 * to start reindexing and updating an existing index. When a indexing process is
 * started it will bock new operation until the ongoing operation is done executing.
 *
 * If update interval is provided it will schedule updates after a successful reindex
 * and after each successful index update.
 */
class IndexProcessor(
    indexer: Indexer,
    indexMaintainer: IndexMaintainer,
    updateInterval: Option[FiniteDuration]
)(
    implicit mat: ActorMaterializer
) extends Actor {

  private implicit val ec: ExecutionContext = context.dispatcher
  private implicit val as: ActorSystem      = context.system

  var indexStatus: IndexStatus         = IndexStatus()
  var indexConfig: Option[IndexConfig] = None
  var nextUpdate: Option[Cancellable]  = None

  override def preStart(): Unit = {
    val aliasExists = indexMaintainer.indexNameForAlias(indexer.indexAliasName)
    aliasExists.foreach(
      optName => {
        indexConfig = optName.map(in => IndexConfig(in, indexer.indexAliasName))
        context.become(ready)
        self.tell(
          indexConfig.map(_ => RequestUpdateIndex).getOrElse(RequestReindex),
          ActorRef.noSender
        )
      }
    )
  }

  override def postStop(): Unit =
    cancelNextUpdate()

  def init: Receive = {
    case _: IndexActorCommand =>
      sender() ! Initialising
    case msg => unhandled(msg)
  }

  def ready: Receive = {
    case RequestUpdateIndex =>
      indexConfig match {
        case Some(name) if indexStatus.canUpdate =>
          indexer.updateExistingIndex(
            name,
            IndexCallback(
              _ => self ! UpdateIndexSuccess,
              () => self ! UpdateIndexFailed
            )
          )
          indexStatus = indexStatus.copy(updateIndexStatus = Executing)
          sender() ! Accepted

        case _ =>
          sender() ! NotAccepted
      }

    case RequestReindex =>
      if (indexStatus.canReindex) {
        indexer.reindexToNewIndex(
          IndexCallback(
            name => self ! ReindexSuccess(name),
            () => self ! ReindexFailed
          )
        )
        indexStatus = indexStatus.copy(reindexStatus = Executing)
        sender() ! Accepted
      } else {
        sender() ! NotAccepted
      }

    case Status =>
      sender() ! nextUpdate.map(_ => ScheduledUpdate).getOrElse(indexStatus.status)

    case UpdateIndexSuccess =>
      indexStatus = indexStatus.copy(updateIndexStatus = IndexSuccess)
      scheduleNextUpdate()

    case UpdateIndexFailed =>
      indexStatus = indexStatus.copy(updateIndexStatus = IndexFailed)

    case ReindexSuccess(newIndexName) =>
      indexStatus = indexStatus.copy(reindexStatus = IndexSuccess)
      indexConfig = Some(newIndexName)
      scheduleNextUpdate()

    case ReindexFailed =>
      indexStatus = indexStatus.copy(reindexStatus = IndexFailed)

    case msg =>
      unhandled(msg)
  }

  private def scheduleNextUpdate(): Unit =
    if (nextUpdate.isEmpty) {
      nextUpdate = updateInterval.map(
        t => context.system.scheduler.scheduleOnce(t, self, RequestUpdateIndex)
      )
    }

  def cancelNextUpdate(): Unit = {
    nextUpdate.foreach(_.cancel())
    nextUpdate = None
  }

  override def receive: Receive = init

}

object IndexProcessor {
  def apply(
      indexer: Indexer,
      indexMaintainer: IndexMaintainer,
      updateInterval: Option[FiniteDuration]
  )(
      implicit mat: ActorMaterializer
  ) =
    Props.apply(classOf[IndexProcessor], indexer, indexMaintainer, updateInterval, mat)

  /**
   * Internal messages protocol
   */
  private[elasticsearch] object InternalProtocol {

    sealed trait InternalActorCommand
    case object UpdateIndexSuccess                    extends InternalActorCommand
    case object UpdateIndexFailed                     extends InternalActorCommand
    case class ReindexSuccess(newConfig: IndexConfig) extends InternalActorCommand
    case object ReindexFailed                         extends InternalActorCommand
  }

  /**
   * Protocol with commands and responses that can be used to interact with the
   * actor.
   */
  object Protocol {

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
    case object Initialising    extends IndexActorStatus
    case object Indexing        extends IndexActorStatus
    case object Ready           extends IndexActorStatus
    case object ScheduledUpdate extends IndexActorStatus
    case object Failed          extends IndexActorStatus
    case object Accepted        extends IndexActorStatus
    case object NotAccepted     extends IndexActorStatus

  }

}

case class IndexStatus(
    reindexStatus: DocumentIndexerStatus = NotExecuted,
    updateIndexStatus: DocumentIndexerStatus = NotExecuted
) {

  def canReindex: Boolean =
    reindexStatus.ready && updateIndexStatus.ready

  def canUpdate: Boolean =
    updateIndexStatus.ready

  def status: IndexActorStatus =
    if (reindexStatus == Executing || updateIndexStatus == Executing)
      Indexing
    else if (updateIndexStatus.ready && reindexStatus.ready)
      Ready
    else
      Failed
}
