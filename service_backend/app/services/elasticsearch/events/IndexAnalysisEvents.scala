package services.elasticsearch.events

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.google.inject.Inject
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.playjson._
import models.analysis.events.{Analysis, AnalysisCollection, SampleCreated}
import models.elasticsearch._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.models.ActorId
import no.uio.musit.time
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import repositories.core.dao.IndexStatusDao
import repositories.elasticsearch.dao.{
  AnalysisEventRow,
  ElasticsearchEventDao,
  ExportEventRow
}
import services.actor.ActorService
import services.elasticsearch._
import services.elasticsearch.shared.ActorEnrichFlow

import scala.concurrent.{Await, ExecutionContext, Future}

class IndexAnalysisEvents @Inject()(
    analysisEventsExportDao: ElasticsearchEventDao,
    indexStatusDao: IndexStatusDao,
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

  val populateActors = new ActorEnrichFlow[ExportEventRow, EventSearch] {
    override def extractActorsId(msg: ExportEventRow): Set[ActorId] =
      findActorIds(msg)

    override def mergeWithActors(
        a: ExportEventRow,
        actors: Set[(ActorId, String)]
    ): EventSearch =
      a match {
        case aer: AnalysisEventRow =>
          aer.event match {
            case a: Analysis           => AnalysisSearch(a, ActorNames(actors))
            case c: AnalysisCollection => AnalysisCollectionSearch(c, ActorNames(actors))
            case sa: SampleCreated     => SampleCreatedSearch(sa, ActorNames(actors))
          }
      }
  }.flow(actorDao, ec)

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

  override def reindexToNewIndex(): Future[IndexName] = {
    val startTime = time.dateTimeNow
    val indexName = createIndexName()
    val source    = analysisEventsExportDao.analysisEventsStream()
    for {
      res <- client.execute(EventIndexConfig.config(indexName.name))
      if res.acknowledged
      _ <- reindex(
            Seq(source.via(populateActors)),
            Some(indexName),
            (_, alias) => indexStatusDao.indexed(alias, startTime).map(_ => ())
          )
    } yield indexName
  }

  override def updateExistingIndex(indexName: IndexName): Future[Unit] =
    for {
      optIndexAfter <- findLastIndexDateTime()
      sources <- optIndexAfter.map { indexAfter =>
                  val s = analysisEventsExportDao.analysisEventsStream(Some(indexAfter))
                  Future(Seq(s))
                }.getOrElse(Future.successful(Seq.empty))
      res <- optIndexAfter.map { startTime =>
              index(
                sources.map(_.via(populateActors)),
                (_, alias) =>
                  indexStatusDao.update(alias, startTime).map {
                    case MusitSuccess(_) => ()
                    case err: MusitError =>
                      logger.error(
                        s"Unable to update index status table. ${err.message}"
                      )
                }
              ).map(_ => ())
            }.getOrElse(Future.successful(()))
    } yield res

  private def findLastIndexDateTime(): Future[Option[DateTime]] = {
    indexStatusDao.findLastIndexed(indexAliasName).map {
      case MusitSuccess(v) => v.map(s => s.updated.getOrElse(s.indexed))
      case _: MusitError   => None
    }
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
