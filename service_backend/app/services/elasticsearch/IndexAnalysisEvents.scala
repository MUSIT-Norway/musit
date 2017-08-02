package services.elasticsearch

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import models.analysis.events.{Analysis, AnalysisCollection, SampleCreated}
import no.uio.musit.models.ActorId
import play.api.Logger
import play.api.libs.json.Json
import repositories.elasticsearch.dao.{
  AnalysisEventRow,
  ElasticsearchEventDao,
  ExportEventRow
}
import services.actor.ActorService
import services.elasticsearch.client.ElasticsearchClient
import services.elasticsearch.client.models.BulkActions.{BulkAction, IndexAction}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}

class IndexAnalysisEvents @Inject()(
    analysisEventsExportDao: ElasticsearchEventDao,
    actorDao: ActorService,
    esClient: ElasticsearchClient,
    client: HttpClient,
    override val indexMaintainer: IndexMaintainer
)(implicit ec: ExecutionContext, mat: Materializer)
    extends Indexer[EventSearch] {

  val logger = Logger(classOf[IndexAnalysisEvents])

  override val indexAliasName = "events"

  override val elasticsearchFlow = new ElasticsearchFlow(esClient, 1000)

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

  override def toAction[B >: BulkAction](
      indexName: IndexName
  ): Flow[EventSearch, B, NotUsed] =
    Flow[EventSearch].map {
      case event: AnalysisSearch =>
        IndexAction(
          indexName.name,
          event.documentType,
          event.documentId,
          Json.toJson(event),
          Some(event.id.underlying.toString)
        )

      case event =>
        IndexAction(
          indexName.name,
          event.documentType,
          event.documentId,
          Json.toJson(event)
        )
    }

  def reindexAll(): Future[Done] = {
    val indexName = createIndexName()
    val source    = Source.fromPublisher(analysisEventsExportDao.analysisEvents())
    for {
      _    <- client.execute(EventIndexConfig.config(indexName.name))
      done <- reindex(source.via(populateActors), Some(indexName))
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
