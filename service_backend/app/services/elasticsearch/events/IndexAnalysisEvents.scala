package services.elasticsearch.events

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.playjson._
import models.analysis.events.{Analysis, AnalysisCollection, SampleCreated}
import models.elasticsearch._
import no.uio.musit.models.ActorId
import play.api.{Configuration, Logger}
import repositories.elasticsearch.dao.{
  AnalysisEventRow,
  ElasticsearchEventDao,
  ExportEventRow
}
import services.actor.ActorService
import services.elasticsearch._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}

class IndexAnalysisEvents @Inject()(
    analysisEventsExportDao: ElasticsearchEventDao,
    actorDao: ActorService,
    client: HttpClient,
    cfg: Configuration,
    override val indexMaintainer: IndexMaintainer
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer[EventSearch] {

  val logger = Logger(classOf[IndexAnalysisEvents])

  override val indexAliasName = "events"

  override val elasticsearchFlow = new ElasticsearchFlow(
    client,
    cfg.getInt("musit.elasticsearch.streams.events.esBatchSize").getOrElse(1000)
  )

  val populateActors =
    new GroupAndEnrichStage[ExportEventRow, EventSearch, ActorId, (ActorId, String)](
      group = findActorIds,
      transform = actors =>
        Await
          .result(
            actorDao.findDetails(actors).map { ps =>
              ps.foldLeft(Map.empty[ActorId, String]) {
                case (state, per) =>
                  val tmp = Map.newBuilder[ActorId, String]
                  per.applicationId.foreach(id => tmp += id -> per.fn)
                  per.dataportenId.foreach(id => tmp += id  -> per.fn)

                  state ++ tmp.result()
              }
            },
            1 minute
          )
          .toSet,
      reducer = (a, s) =>
        a match {
          case aer: AnalysisEventRow =>
            aer.event match {
              case a: Analysis           => AnalysisSearch(a, ActorNames(s))
              case c: AnalysisCollection => AnalysisCollectionSearch(c, ActorNames(s))
              case sa: SampleCreated     => SampleCreatedSearch(sa, ActorNames(s))
            }
      },
      limit = 10
    )

  override def toAction(
      indexName: IndexName
  ): Flow[EventSearch, BulkCompatibleDefinition, NotUsed] =
    Flow[EventSearch].map {
      case event: AnalysisSearch =>
        val action = indexInto(indexName.name, event.documentType) id event.documentId doc event
        event.partOf.map(action parent _.underlying.toString).getOrElse(action)
      case event =>
        indexInto(indexName.name, event.documentType) id event.documentId doc event
    }

  def reindexAll(): Future[Done] = {
    val indexName = createIndexName()
    val source    = Source.fromPublisher(analysisEventsExportDao.analysisEventsStream())
    for {
      _    <- client.execute(EventIndexConfig.config(indexName.name))
      done <- reindex(Seq(source.via(populateActors)), Some(indexName))
    } yield done
  }

  def findActorIds(event: ExportEventRow): Set[ActorId] = event match {
    case evt: AnalysisEventRow =>
      evt.event match {
        case a: Analysis =>
          Set(
            a.completedBy,
            a.doneBy,
            a.administrator,
            a.responsible,
            a.registeredBy,
            a.updatedBy,
            a.result.flatMap(_.registeredBy)
          ).flatten

        case a: AnalysisCollection =>
          Set(
            a.completedBy,
            a.doneBy,
            a.administrator,
            a.responsible,
            a.registeredBy,
            a.updatedBy,
            a.restriction.map(_.requester)
          ).flatten

        case s: SampleCreated =>
          Set(s.doneBy, s.registeredBy).flatten
      }
  }

}
