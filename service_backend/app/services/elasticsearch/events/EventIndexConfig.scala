package services.elasticsearch.events

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import services.elasticsearch.shared.FieldConfig._

object EventIndexConfig {

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(analysisType) as (
        intField("id"),
        intField("analysisTypeId"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        actorStamp("responsible"),
        actorStamp("administrator"),
        actorSearchStamp("updatedBy"),
        actorSearchStamp("completedBy"),
        uuid("objectId"),
        textField("objectType"),
        textField("partOf"),
        textField("note"),
        extraAttributes,
        objectField("result") fields size
      ) parent analysisCollectionType,
      mapping(analysisCollectionType) as (
        intField("id"),
        intField("analysisTypeId"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        actorStamp("responsible"),
        actorStamp("administrator"),
        actorSearchStamp("updatedBy"),
        actorSearchStamp("completedBy"),
        textField("note"),
        extraAttributes,
        objectField("result") fields size,
        textField("reason"),
        intField("status"),
        textField("orgId")
      ),
      mapping(sampleType) as (
        intField("id"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        uuid("objectId"),
        uuid("sampleObjectId"),
        textField("externalLinks")
      )
    )

  private def extraAttributes = {
    objectField("extraAttributes") fields (
      textField("method"), // this is an integer for all except ExtractionAttributes
      textField("types"),
      textField("extractionType")
    )
  }

}
