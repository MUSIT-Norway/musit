package services.elasticsearch.index

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import models.elasticsearch.{IndexCallback, IndexConfig}
import no.uio.musit.functional.FutureMusitResult
import org.joda.time.DateTime
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import repositories.core.dao.IndexStatusDao
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

  def createDummyIndexer(maintainer: IndexMaintainer) =
    new DummyIndexer(maintainer, null)

  "IndexProcessor" should {

    "give `Indexing` status when reindexing is running" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
    }

    "give `Ready` status when indexing is done" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerReindexSuccess()
      eventuallyStatus(ref, Ready)
    }

    "give `Ready` status when index exists" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerUpdateIndexSuccess()
      eventuallyStatus(ref, Ready)
    }

    "give `Accepted` status when not indexing on `RequestReindex` command" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerReindexSuccess()
      eventuallyStatus(ref, Ready)

      ref ! RequestReindex
      expectMsg(Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestReindex` command" in {
      val maintainer = new DummyIndexMaintainer(false)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)

      ref ! RequestReindex
      expectMsg(NotAccepted)
    }

    "give `Accepted` status when not indexing on `RequestUpdateIndex` command" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)
      indexer.triggerUpdateIndexSuccess()
      eventuallyStatus(ref, Ready)

      ref ! RequestUpdateIndex
      expectMsg(Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestUpdateIndex` command" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = createDummyIndexer(maintainer)
      val ref        = system.actorOf(IndexProcessor(indexer, maintainer, None, 1 millis))

      eventuallyStatus(ref, Indexing)

      ref ! RequestUpdateIndex
      expectMsg(NotAccepted)
    }

    "give `ScheduleUpdate` status when scheduled" in {
      val maintainer = new DummyIndexMaintainer(true)
      val indexer    = createDummyIndexer(maintainer)
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

  class DummyIndexer(
      val indexMaintainer: IndexMaintainer,
      override val indexStatusDao: IndexStatusDao
  ) extends Indexer {
    override val indexAliasName: String = "dummy"
    override val client: HttpClient     = null

    private[this] var indexCallbackOpt: Option[IndexCallback]  = None
    private[this] var updateCallbackOpt: Option[IndexCallback] = None

    override def createIndex()(implicit ec: ExecutionContext): Future[IndexConfig] = {
      Future.successful(createIndexConfig())
    }

    override def reindexDocuments(
        indexConfig: IndexConfig,
        indexCallback: IndexCallback
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
      indexCallbackOpt.foreach(_.onSuccess(IndexConfig("dummy_index", indexAliasName)))
      indexCallbackOpt = None
    }

    def triggerUpdateIndexSuccess(): Unit = {
      updateCallbackOpt.foreach(_.onSuccess(IndexConfig("dummy_index", indexAliasName)))
      updateCallbackOpt = None
    }

    def createIndexMapping(indexName: String)(
        implicit ec: ExecutionContext
    ): CreateIndexDefinition =
      throw new IllegalStateException(
        "Not meant to be calling DummyIndexer.createIndexMapping"
      )

    def createElasticSearchBulkSource(
        config: IndexConfig,
        eventsAfter: Option[DateTime]
    )(
        implicit ec: ExecutionContext
    ): FutureMusitResult[Source[BulkCompatibleDefinition, NotUsed]] =
      throw new IllegalStateException(
        "Not meant to be calling DummyIndexer.createElasticSearchBulkSource"
      )
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
