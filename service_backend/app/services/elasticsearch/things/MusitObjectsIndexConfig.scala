package services.elasticsearch.things

import com.sksamuel.elastic4s.http.ElasticDsl.createIndex
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import services.elasticsearch.shared.FieldConfig._

object MusitObjectsIndexConfig {

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(collectionType) as (
        uuid("id"),
        textField("museumId"),
        textField("museumNo"),
        textField("subNo"),
        textField("term"),
        longField("mainObjectId"),
        objectField("collection") fields (
          intField("id"),
          uuid("uuid")
        ),
        textField("arkForm"),
        textField("arkFindingNo"),
        textField("natStage"),
        textField("natGender"),
        textField("natLegDate")
      ),
      mapping(sampleType) as (
        uuid("objectId"),
        uuid("originatedObjectUuid"),
        objectField("parentObject") fields (
          textField("objectId"),
          textField("objectType")
        ),
        booleanField("isExtracted"),
        textField("museumId"),
        intField("status"),
        actorStamp("responsible"),
        actorSearchStamp("doneByStamp"),
        intField("sampleNum"),
        textField("sampleId"),
        objectField("externalId") fields (
          textField("value"),
          textField("source")
        ),
        intField("sampleTypeId"),
        size,
        textField("container"),
        textField("storageMedium"),
        textField("treatment"),
        intField("leftoverSample"),
        textField("description"),
        textField("note"),
        actorSearchStamp("registeredStamp"),
        actorSearchStamp("updatedStamp")
      ) parent collectionType
    )
}
