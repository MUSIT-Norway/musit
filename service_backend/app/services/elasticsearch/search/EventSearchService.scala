package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import org.apache.lucene.search.join.ScoreMode
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.events.{
  analysisCollectionType,
  analysisType,
  indexAlias,
  sampleType
}

import scala.concurrent.{ExecutionContext, Future}

class EventSearchService @Inject()(implicit client: HttpClient, ex: ExecutionContext) {

  def restrictedEventsSearch(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      queryStr: Option[String]
  )(implicit currUsr: AuthenticatedUser): Future[MusitESResponse[SearchResponse]] = {
    val qry: QueryDefinition = createQuery(mid, collectionIds, queryStr.getOrElse("*"))

    client.execute(
      search(
        IndexAndTypes(indexAlias, Seq(analysisType, analysisCollectionType, sampleType))
      ) query qry
    )(MusitSearchHttpExecutable.musitSearchHttpExecutable)
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
        "analysis_collection_parent"
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
      ) innerHit innerHits("analysis_children"),
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
