package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import no.uio.musit.security.AuthenticatedUser
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.events.{
  indexAlias,
  analysisType,
  analysisCollectionType,
  sampleType
}

import scala.concurrent.{ExecutionContext, Future}

class EventSearchService @Inject()(implicit client: HttpClient, ex: ExecutionContext) {

  def restrictedEventsSearch(
      q: Option[String]
  )(implicit currUsr: AuthenticatedUser): Future[MusitESResponse[SearchResponse]] = {
    val qry: QueryDefinition = createQuery(q)

    client.execute(
      search(
        IndexAndTypes(indexAlias, Seq(analysisType, analysisCollectionType, sampleType))
      ) query qry
    )(MusitSearchHttpExecutable.musitSearchHttpExecutable)
  }

  private def createQuery(q: Option[String])(
      implicit currUsr: AuthenticatedUser
  ): QueryDefinition = ???
}
