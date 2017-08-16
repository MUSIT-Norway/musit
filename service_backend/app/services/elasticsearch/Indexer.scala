package services.elasticsearch

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

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
   * Tha actual index that we will use. We will hide this behind an alias. That's why
   * we prefix it with the alias name.
   */
  def createIndexName(): IndexName =
    IndexName(s"${indexAliasName}_${System.currentTimeMillis()}")

  /**
   * Reindex all documents to index
   */
  def reindexToNewIndex(indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit

  /**
   * Update the existing index with updated and new documents
   */
  def updateExistingIndex(index: IndexName, indexCallback: IndexCallback)(
      implicit ec: ExecutionContext,
      mat: Materializer,
      as: ActorSystem
  ): Unit

}

case class IndexName(underlying: String) {
  override def toString = underlying

  def name = underlying
}

case class IndexCallback(success: IndexName => Unit, failure: () => Unit)
