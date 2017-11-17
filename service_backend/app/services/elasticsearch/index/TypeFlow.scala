package services.elasticsearch.index

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import models.elasticsearch.{IndexConfig, Searchable}

/**
 * The flow step for a type in an index.
 * It takes in a source document and convert it to a document that will be indexed into
 * elasticsearch. When this is done it will be converted to a bulk operation before it's
 * sent to elasticsearch.
 */
trait TypeFlow[SourceDoc, Doc <: Searchable] {

  /**
   * This Flow step take the raw documents from the source and populate them with the
   * required content.
   */
  def populateWithData(
      indexConfig: IndexConfig
  ): Flow[SourceDoc, Doc, NotUsed]

  /**
   * Flow to convert the document that should be indexed to a bulk operation.
   */
  def toBulkDefinitions(
      indexConfig: IndexConfig
  ): Flow[Doc, BulkCompatibleDefinition, NotUsed]

  /**
   * This is the flow that will be used in the Indexer.
   */
  def flow(
      indexConfig: IndexConfig
  ): Flow[SourceDoc, BulkCompatibleDefinition, NotUsed] =
    populateWithData(indexConfig).via(toBulkDefinitions(indexConfig))

}
