package services.elasticsearch

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition

import scala.concurrent.{ExecutionContext, Future}

trait Indexer[S] {

  /**
   * The index alias name.
   */
  val indexAliasName: String

  /**
   * Swaps the old alias with the new index.
   */
  val indexMaintainer: IndexMaintainer

  /**
   * The elasticsearch flow that is configured for the source.
   */
  val elasticsearchFlow: ElasticsearchFlow

  /**
   * Convert an element to an action that will be executed by elestic search
   */
  def toAction(index: IndexName): Flow[S, BulkCompatibleDefinition, NotUsed]

  /**
   * Tha actual index that we will use. We will hide this behind an alias. That's why
   * we prefix it with the alias name.
   */
  def createIndexName(): IndexName =
    IndexName(s"${indexAliasName}_${System.currentTimeMillis()}")

  /**
   * When we're creating a new index or reindex an old one we want to keep the old index
   * active until the new one is up and running. This can take some time depending on the
   * size of the source.
   */
  def reindex[B](
      source: Source[S, B],
      indexName: Option[IndexName] = None
  )(implicit mat: Materializer, ec: ExecutionContext): Future[Done] = {
    index(source, { (newIndex, alias) =>
      indexMaintainer.activateIndex(newIndex.name, alias)
    }, indexName)
  }

  /**
   * Index the source and run `onComplete` when executed successfully.
   */
  def index[B](
      source: Source[S, B],
      onComplete: (IndexName, String) => Future[Unit] = (_, _) => Future.successful(()),
      indexName: Option[IndexName] = None
  )(implicit mat: Materializer, ec: ExecutionContext): Future[Done] = {
    val newIndex = indexName.getOrElse(createIndexName())
    source
      .via(toAction(newIndex))
      .via(elasticsearchFlow.flow())
      .runWith(Sink.ignore)
      .flatMap(done => onComplete(newIndex, indexAliasName).map(_ => done))
  }

}

case class IndexName(underlying: String) {
  override def toString = underlying
  def name              = underlying
}
