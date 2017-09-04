package services.elasticsearch.index

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import models.elasticsearch.{IndexCallback, IndexConfig}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import services.elasticsearch.index.IndexProcessor.Protocol._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class IndexProcessorSpec
    extends TestKit(ActorSystem("IndexActorSpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with MustMatchers
    with Eventually {

  implicit val mat = ActorMaterializer()

  override def afterAll = {
    mat.shutdown()
    shutdown(system)
  }

  "IndexProcessor" should {

    "give `Indexing` status when reindexing is running" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
    }

    "give `Ready` status when indexing is done" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerReindexSuccess()
      eventuallyStatus(ref, Ready)
    }

    "give `Ready` status when index exists" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerUpdateIndexSuccess()
      eventuallyStatus(ref, Ready)
    }

    "give `Accepted` status when not indexing on `RequestReindex` command" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerReindexSuccess()
      eventuallyStatus(ref, Ready)

      ref ! RequestReindex
      expectMsg(Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestReindex` command" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)

      ref ! RequestReindex
      expectMsg(NotAccepted)
    }

    "give `Accepted` status when not indexing on `RequestUpdateIndex` command" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerUpdateIndexSuccess()
      eventuallyStatus(ref, Ready)

      ref ! RequestUpdateIndex
      expectMsg(Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestUpdateIndex` command" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = new DummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)

      ref ! RequestUpdateIndex
      expectMsg(NotAccepted)
    }

    "give `ScheduleUpdate` status when scheduled" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = new DummyIndexer(maintainer)
      val ref =
        system.actorOf(IndexProcessor(indexer, maintainer, Some(1 minute), 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerUpdateIndexSuccess()

      eventuallyStatus(ref, ScheduledUpdate)
    }
  }

  private def eventuallyStatus(ref: ActorRef, status: IndexActorStatus) = {
    val tp = TestProbe()
    eventually(Timeout(3 seconds)) {
      ref.tell(Status, tp.ref)
      tp.expectMsg(status)
    }
  }

  class DummyIndexer(val indexMaintainer: IndexMaintainer) extends Indexer {
    override val indexAliasName: String = "dummy"

    private[this] var indexCallbackOpt: Option[IndexCallback]  = None
    private[this] var updateCallbackOpt: Option[IndexCallback] = None

    override def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig] = {
      Future.successful(createIndexConfig())
    }

    override def reindexDocuments(
        indexCallback: IndexCallback,
        indexConfig: IndexConfig
    )(implicit ec: ExecutionContext, mat: Materializer, as: ActorSystem): Unit = {
      indexCallbackOpt = Some(indexCallback)
    }

    override def updateExistingIndex(
        indexConfig: IndexConfig,
        indexCallback: IndexCallback
    )(implicit ec: ExecutionContext, mat: Materializer, as: ActorSystem): Unit = {
      updateCallbackOpt = Some(indexCallback)
    }

    def triggerReindexSuccess(): Unit = {
      indexCallbackOpt.foreach(_.success(IndexConfig("dummy_index", indexAliasName)))
      indexCallbackOpt = None
    }

    def triggerUpdateIndexSuccess(): Unit = {
      updateCallbackOpt.foreach(_.success(IndexConfig("dummy_index", indexAliasName)))
      updateCallbackOpt = None
    }
  }

  class DummyIndexMaintainer(hasIndex: Boolean) extends IndexMaintainer(null) {
    override def activateIndex(index: String, aliasName: String)(
        implicit ec: ExecutionContext
    ): Future[Unit] =
      Future.successful(())

    override def indexNameForAlias(
        alias: String
    )(implicit ec: ExecutionContext): Future[Option[String]] =
      Future.successful(if (hasIndex) Some("dummy_old_index") else None)
  }

}
