package services.elasticsearch.index

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import models.elasticsearch.DocumentIndexerStatuses._
import models.elasticsearch.{IndexCallback, IndexConfig}
import services.elasticsearch.index.IndexProcessor.InternalProtocol._
import services.elasticsearch.index.IndexProcessor.Protocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

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
    initDelay: FiniteDuration,
    updateInterval: Option[FiniteDuration]
)(
    implicit mat: ActorMaterializer
) extends Actor
    with ActorLogging {

  private implicit val ec: ExecutionContext = context.dispatcher
  private implicit val as: ActorSystem      = context.system
  private implicit val to: Timeout          = Timeout(10 seconds)

  private val name = self.path.name

  var indexStatus: IndexStatus         = IndexStatus()
  var indexConfig: Option[IndexConfig] = None
  var nextUpdate: Option[Cancellable]  = None

  override def preStart(): Unit = {
    log.info(s"[$name]: Setting up actor")
    as.scheduler.scheduleOnce(initDelay) { initIndex() }
  }

  private def initIndex(): Future[Unit] = {
    log.info(s"[$name]: Checking status for index")
    indexMaintainer
      .indexNameForAlias(indexer.indexAliasName)
      .flatMap(
        optName => {
          indexConfig = optName.map(in => IndexConfig(in, indexer.indexAliasName))
          indexConfig match {
            case Some(in) => log.info(s"[$name]: Found existing index ($in). Updating it")
            case None     => log.info(s"[$name]: No index found. Creating a new one.")
          }
          context.become(ready)
          (self ? indexConfig.map(_ => RequestUpdateIndex).getOrElse(RequestReindex))
            .map(res => log.info(s"[$name]: Response on request: $res"))
        }
      )
      .recover {
        case NonFatal(t) =>
          log.error(t, s"[$name]: Failed initialise index, will try again later")
          as.scheduler.scheduleOnce(2 minutes) { initIndex() }
      }
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
      nextUpdate = None
      indexConfig match {
        case Some(indexName) if indexStatus.canUpdate =>
          indexer.updateExistingIndex(
            indexName,
            IndexCallback(
              _ => self ! UpdateIndexSuccess,
              t => self ! UpdateIndexFailed(t)
            )
          )
          indexStatus = indexStatus.copy(updateIndexStatus = Executing)
          sender() ! Accepted
          log.info(s"[$name]: Updating index")

        case _ =>
          log.warning(s"RequestUpdateIndex Cannot reindex: $name")
          sender() ! NotAccepted
      }

    case RequestReindex =>
      if (indexStatus.canReindex) {
        indexer.reindexToNewIndex(
          IndexCallback(
            name => self ! ReindexSuccess(name),
            t => self ! ReindexFailed
          )
        )
        indexStatus = indexStatus.copy(reindexStatus = Executing)
        log.info(s"[$name]: Reindexing")
        sender() ! Accepted
      } else {
        log.warning(s"RequestReindex Cannot reindex: $name")
        sender() ! NotAccepted
      }

    case Status =>
      sender() ! nextUpdate.map(_ => ScheduledUpdate).getOrElse(indexStatus.status)

    case UpdateIndexSuccess =>
      indexStatus = indexStatus.copy(updateIndexStatus = IndexSuccess)
      scheduleNextUpdateIndex()
      log.info(s"[$name]: Index is up to date")

    case UpdateIndexFailed(t) =>
      indexStatus = indexStatus.copy(updateIndexStatus = IndexFailed)
      scheduleNextUpdateIndex()
      log.error(t, s"[$name]: Failed to update index")

    case ReindexSuccess(newIndexName) =>
      indexStatus = indexStatus.copy(reindexStatus = IndexSuccess)
      indexConfig = Some(newIndexName)
      scheduleNextUpdateIndex()
      log.info(s"[$name]: Reindex is done")

    case ReindexFailed(t) =>
      indexStatus = indexStatus.copy(reindexStatus = IndexFailed)
      scheduleNextUpdateIndex()
      log.error(t, s"[$name]: Reindex failed")

    case msg =>
      unhandled(msg)
  }
  /*Schedules a RequestUpdateIndex in the future*/
  private def scheduleNextUpdateIndex(): Unit = {

    /*  nextUpdate is cleared at the start of RequestUpdateIndex.
        Alternatively we could perhaps just as well remove this guard and ignore clearing
        nextUpdate at the beginning of RequestUpdateIndex.
        (The current logic is a bit safer if scheduleNextUpdate is called "too often".)
     */

    if (nextUpdate.isEmpty) {
      nextUpdate = updateInterval.map(
        t => context.system.scheduler.scheduleOnce(t, self, RequestUpdateIndex)
      )
    }
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
      updateInterval: Option[FiniteDuration],
      initDelay: FiniteDuration = 10 seconds
  )(
      implicit mat: ActorMaterializer
  ) =
    Props.apply(
      classOf[IndexProcessor],
      indexer,
      indexMaintainer,
      initDelay,
      updateInterval,
      mat
    )

  /**
   * Internal messages protocol
   */
  private[elasticsearch] object InternalProtocol {

    sealed trait InternalActorCommand
    case object UpdateIndexSuccess                    extends InternalActorCommand
    case class UpdateIndexFailed(reason: Throwable)   extends InternalActorCommand
    case class ReindexSuccess(newConfig: IndexConfig) extends InternalActorCommand
    case class ReindexFailed(reason: Throwable)       extends InternalActorCommand
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
