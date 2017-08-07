package services.elasticsearch.things

import com.sksamuel.elastic4s.http.ElasticDsl.createIndex
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._

object MusitObjectsIndexConfig {

  val collectionType = "collection"

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(collectionType) as (
        textField("id"),
        textField("museumId"),
        textField("museumNo"),
        textField("subNo"),
        textField("term"),
        longField("mainObjectId"),
        objectField("collection") fields (
          intField("id"),
          textField("uuid")
        ),
        textField("arkForm"),
        textField("arkFindingNo"),
        textField("natStage"),
        textField("natGender"),
        textField("natLegDate")
      )
    )
}
