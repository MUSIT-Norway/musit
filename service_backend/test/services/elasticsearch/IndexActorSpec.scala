package services.elasticsearch

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import services.elasticsearch.DocumentIndexer._
import services.elasticsearch.IndexActor._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class IndexActorSpec
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

  "IndexActor" should {

    "give `Indexing` status when reindexing is running" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = false,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      indexer.reindexStatus = Executing
      eventuallyStatus(ref, Indexing)
    }

    "give `Ready` status when indexing is done" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = false,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      indexer.reindexStatus = IndexSuccess
      eventuallyStatus(ref, Ready)
    }

    "give `Ready` status when index exists" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = true,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      eventuallyStatus(ref, Ready)
    }

    "give `Accepted` status when not indexing on `RequestReindex` command" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = true,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      eventuallyStatus(ref, Ready)
      ref ! RequestReindex
      eventuallyStatus(ref, Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestReindex` command" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = true,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      eventuallyStatus(ref, Ready)
      indexer.reindexStatus = Executing
      ref ! RequestReindex
      eventuallyStatus(ref, NotAccepted)
    }

    "give `Accepted` status when not indexing on `RequestLiveIndex` command" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = true,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      eventuallyStatus(ref, Ready)
      ref ! RequestLiveIndex
      eventuallyStatus(ref, Accepted)
    }

    "give `NotAccepted` status when indexing on `RequestLiveIndex` command" in {
      val indexer = DummyDocumentIndexer(
        hasIndex = true,
        reindexStatus = NotExecuted,
        updateIndexStatus = NotExecuted
      )
      val ref = system.actorOf(IndexActor(indexer))

      eventuallyStatus(ref, Ready)
      indexer.reindexStatus = Executing
      ref ! RequestLiveIndex
      eventuallyStatus(ref, NotAccepted)
    }
  }

  private def eventuallyStatus(ref: ActorRef, status: IndexActorStatus) = {
    eventually(Timeout(3 seconds)) {
      ref ! Status
      expectMsg(status)
    }
  }

  case class DummyDocumentIndexer(
      hasIndex: Boolean,
      var reindexStatus: DocumentIndexerStatus,
      var updateIndexStatus: DocumentIndexerStatus
  ) extends DocumentIndexer {

    var reindexCalled     = false
    var updadeIndexCalled = false
    override def initIndex()(implicit ec: ExecutionContext): Future[Boolean] =
      Future.successful(hasIndex)

    override def reindex()(implicit ec: ExecutionContext, mat: Materializer): Unit = {
      reindexCalled = true
    }

    override def updateIndex()(implicit ec: ExecutionContext, mat: Materializer): Unit = {
      updadeIndexCalled = true
    }

  }
}
