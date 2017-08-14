package services.elasticsearch.things

import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch.{MusitObjectSearch, MustObjectSearch}
import models.musitobject.MusitObject
import services.elasticsearch.{IndexConfig, TypeFlow}

class MusitObjectTypeFlow extends TypeFlow[MusitObject, MusitObjectSearch] {
  override def flow(indexConfig: IndexConfig) =
    Flow[MusitObject]
      .filter(_.uuid.isDefined)
      .map(mObj => MustObjectSearch(mObj))
      .via(toBulkDefinitions(indexConfig))

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[MusitObjectSearch].map { thing =>
      indexInto(indexConfig.indexName, thing.documentType) id thing.documentId doc thing
    }

}
