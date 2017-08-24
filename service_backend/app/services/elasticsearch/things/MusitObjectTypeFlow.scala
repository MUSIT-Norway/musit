package services.elasticsearch.things

import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.playjson._
import models.elasticsearch.{IndexConfig, MusitObjectSearch}
import models.musitobject.MusitObject
import services.elasticsearch.TypeFlow

class MusitObjectTypeFlow extends TypeFlow[MusitObject, MusitObjectSearch] {
  override def populateWithData(indexConfig: IndexConfig) =
    Flow[MusitObject].filter(_.uuid.isDefined).map(mObj => MusitObjectSearch(mObj))

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[MusitObjectSearch].map { thing =>
      indexInto(indexConfig.name, objectType) id thing.docId doc thing
    }

}
