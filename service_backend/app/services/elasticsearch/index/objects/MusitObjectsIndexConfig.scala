package services.elasticsearch.index.objects

import com.sksamuel.elastic4s.http.ElasticDsl.createIndex
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import services.elasticsearch.index.shared.FieldConfig._

object MusitObjectsIndexConfig {

  def config(indexName: String): CreateIndexDefinition =
    createIndex(indexName) mappings (
      mapping(objectType) as (
        uuid("id"),
        intField("museumId"),
        keywordField("museumNo"),
        keywordField("subNo"),
        textField("term"),
        longField("mainObjectId"),
        collection,
        textField("arkForm"),
        textField("arkFindingNo"),
        textField("natStage"),
        textField("natGender"),
        textField("natLegDate"),
        booleanField("isDeleted")
      ),
      mapping(sampleType) as (
        uuid("objectId"),
        uuid("originatedObjectUuid"),
        objectField("parentObject") fields (
          uuid("objectId"),
          textField("objectType")
        ),
        booleanField("isExtracted"),
        intField("museumId"),
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
        actorSearchStamp("updatedStamp"),
        booleanField("isDeleted")
      ) parent objectType
    )
}
