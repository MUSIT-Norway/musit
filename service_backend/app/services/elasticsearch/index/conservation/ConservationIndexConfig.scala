package services.elasticsearch.index.conservation

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import services.elasticsearch.index.shared.FieldConfig._

object ConservationIndexConfig {

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(conservationType) as (
        intField("eventId"),
        intField("eventTypeId"),
        intField("museumId"), //Why is this defined as textField in Analysis?
//        actorSearchStamp("registeredBy"),
//        actorSearchStamp("updatedBy"),
        intField("partOf"),
        textField("note"),
        textField("caseNumber"),
        uuid("collectionUuid"),
        jsonExtraAttributes
      )
    )

  private def jsonExtraAttributes = {
    objectField("eventJson") /* fields (
    //TODO
//      textField("method"), // this is an integer for all except ExtractionAttributes
//      textField("types")
//      textField("extractionType")
    ) */
  }

}
