package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import models.conservation.events.ConservationType
import models.elasticsearch.Constants
import no.uio.musit.MusitResults.{MusitGeneralError, MusitResult, MusitSuccess}
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import org.apache.lucene.search.join.ScoreMode
import play.api.Logger
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.conservation._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ConservationSearchService @Inject()(
    implicit client: HttpClient,
    ex: ExecutionContext
) {

  private[this] val logger = Logger(classOf[ConservationSearchService])

  def restrictedConservationSearch(
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
          IndexAndTypes(
            indexAlias,
            conservationType
          )
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

    val onlyAllowedConservationQuery = must(
      restrictToCollectionAndMuseumQuery(mid, collectionIds, Constants.collectionUuid),
      termQuery("_type" -> conservationType)
    )

    must(
      should(
        onlyAllowedConservationQuery
      )
    ).appendMust(baseQuery)

  }

}
