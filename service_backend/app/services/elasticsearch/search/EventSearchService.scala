package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import no.uio.musit.MusitResults.{MusitGeneralError, MusitResult, MusitSuccess}
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import org.apache.lucene.search.join.ScoreMode
import play.api.Logger
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.events.{
  analysisCollectionType,
  analysisType,
  indexAlias,
  sampleType
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EventSearchService @Inject()(implicit client: HttpClient, ex: ExecutionContext) {

  private[this] val logger = Logger(classOf[EventSearchService])

  def restrictedEventsSearch(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      from: Int,
      limit: Int,
      queryStr: Option[String]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[MusitESResponse[SearchResponse]]] = {
    val qry: QueryDefinition = createQuery(mid, collectionIds, queryStr.getOrElse("*"))

    client
      .execute(
        search(
          IndexAndTypes(indexAlias, Seq(analysisType, analysisCollectionType, sampleType))
        ) query qry from from limit limit
      )(MusitSearchHttpExecutable.musitSearchHttpExecutable)
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(err) =>
          val msg = s"Unable to execute search: ${err.getMessage}"
          logger.warn(msg, err)
          MusitGeneralError(msg)
      }
  }

  private def createQuery(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      queryStr: String
  )(
      implicit currUsr: AuthenticatedUser
  ): QueryDefinition = {

    val baseQuery = query(queryStr)

    val onlyAllowedAnalysisQuery = must(
      restrictToCollectionAndMuseumQuery(mid, collectionIds),
      hasParentQuery(analysisCollectionType, matchAllQuery(), score = false) innerHit innerHits(
        EventSearchService.analyseInnerHitName
      ),
      termQuery("_type" -> analysisType)
    )
    val onlyAllowedCreateSampleQuery = must(
      restrictToCollectionAndMuseumQuery(mid, collectionIds),
      termQuery("_type" -> sampleType)
    )
    val onlyAllowedAnalysisCollectionQuery = must(
      hasChildQuery(
        analysisType,
        restrictToCollectionAndMuseumQuery(mid, collectionIds),
        ScoreMode.None
      ) innerHit innerHits(EventSearchService.analysisCollectionInnerHitName),
      termQuery("_type" -> analysisCollectionType)
    )

    must(
      should(
        onlyAllowedAnalysisQuery,
        onlyAllowedAnalysisCollectionQuery,
        onlyAllowedCreateSampleQuery
      ),
      should(
        hasChildQuery(analysisType, must(baseQuery), ScoreMode.None),
        must(baseQuery)
      )
    )
  }

}

object EventSearchService {

  val analyseInnerHitName            = "analysisCollection"
  val analysisCollectionInnerHitName = "analysis"
}
