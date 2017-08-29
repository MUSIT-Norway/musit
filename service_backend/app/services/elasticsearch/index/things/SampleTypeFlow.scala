package services.elasticsearch.index.things

import akka.stream.scaladsl.Flow
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.playjson._
import models.analysis.SampleObject
import models.elasticsearch.{ActorNames, IndexConfig, SampleObjectSearch}
import no.uio.musit.models.ActorId
import services.actor.ActorService
import services.elasticsearch.index.TypeFlow
import services.elasticsearch.index.shared.ActorEnrichFlow

import scala.concurrent.ExecutionContext

class SampleTypeFlow(actorService: ActorService)(implicit ec: ExecutionContext)
    extends TypeFlow[SampleObject, SampleObjectSearch] {

  private val withActorNames = new ActorEnrichFlow[SampleObject, SampleObjectSearch] {
    override def extractActorsId(sample: SampleObject) =
      Set(
        sample.registeredStamp.map(_.user),
        sample.updatedStamp.map(_.user),
        sample.doneByStamp.map(_.user),
        sample.responsible
      ).flatten

    override def mergeWithActors(input: SampleObject, actors: Set[(ActorId, String)]) =
      SampleObjectSearch(input, ActorNames(actors))
  }.flow(actorService, ec)

  override def populateWithData(indexConfig: IndexConfig) =
    withActorNames

  override def toBulkDefinitions(indexConfig: IndexConfig) =
    Flow[SampleObjectSearch].map { sample =>
      indexInto(indexConfig.name, "sample") id sample.docId doc sample parent
        sample.docParentId.get //does always have a value
    }

}
