package services.elasticsearch

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import models.elasticsearch.IndexConfig

/**
 * The flow step for a type in an index.
 */
trait TypeFlow[Input, Doc] {

  def flow(
      indexConfig: IndexConfig
  ): Flow[Input, BulkCompatibleDefinition, NotUsed]

  def toBulkDefinitions(
      indexConfig: IndexConfig
  ): Flow[Doc, BulkCompatibleDefinition, NotUsed]

}
