package services.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition

object EventIndexConfig {

  val analysisType           = "analysis"
  val analysisCollectionType = "analysisCollection"
  val sampleType             = "sample"

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(analysisType) as (
        textField("id"),
        intField("analysisTypeId"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        actorStamp("responsible"),
        actorStamp("administrator"),
        actorSearchStamp("updatedBy"),
        actorSearchStamp("completedBy"),
        textField("objectId"),
        textField("objectType"),
        textField("partOf"),
        textField("note"),
        objectField("extraAttributes") fields (
          textField("method"),
          textField("types")
        ),
        objectField("result") fields size
      ) parent analysisCollectionType,
      mapping(analysisCollectionType) as (
        textField("id"),
        intField("analysisTypeId"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        actorStamp("responsible"),
        actorStamp("administrator"),
        actorSearchStamp("updatedBy"),
        actorSearchStamp("completedBy"),
        textField("note"),
        objectField("extraAttributes") fields (
          textField("method"),
          textField("types")
        ),
        objectField("result") fields size,
        textField("reason"),
        intField("status"),
        textField("orgId")
      ),
      mapping(sampleType) as (
        textField("id"),
        actorSearchStamp("doneBy"),
        actorSearchStamp("registeredBy"),
        textField("objectId"),
        textField("sampleObjectId")
        //externalLinks
      )
    )

  private def actorSearchStamp(name: String) =
    objectField(name) fields (
      textField("id"),
      textField("date"),
      textField("name")
    )

  private def actorStamp(name: String) =
    objectField(name) fields (textField("id"),
    textField("name"))

  private def size = {
    objectField("size") fields (
      textField("unit"),
      doubleField("value")
    )
  }

}
