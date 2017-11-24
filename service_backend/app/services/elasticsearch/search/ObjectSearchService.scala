package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl.{termQuery, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import no.uio.musit.MusitResults._
import no.uio.musit.models.{MuseumCollection, MuseumId, MuseumNo, SubNo}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.objects.{indexAlias, objectType, sampleType}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ObjectSearchService @Inject()(implicit client: HttpClient, ex: ExecutionContext) {

  private[this] val logger = Logger(classOf[ObjectSearchService])

  def restrictedObjectSearch(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      from: Int,
      limit: Int,
      // query stuff
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      queryStr: Option[String]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[MusitESResponse[SearchResponse]]] = {
    val qry = createSearchQuery(mid, collectionIds, museumNo, subNo, term, queryStr)

    client
      .execute(
        search(IndexAndTypes(indexAlias, Seq(objectType, sampleType)))
          query qry
          limit limit
          from from
      )(MusitSearchHttpExecutable.musitSearchHttpExecutable)
      .map(MusitSuccess.apply)
      .recover {
        case NonFatal(err) =>
          val msg = s"Unable to execute search: ${err.getMessage}"
          logger.warn(msg, err)
          MusitGeneralError(msg)
      }
  }

  private def createSearchQuery(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String],
      q: Option[String]
  )(implicit currUsr: AuthenticatedUser) = {
    val objectTypeFilter = Seq(
      museumNo.map(v => wildcardQuery("museumNo", v.value.toLowerCase)),
      subNo.map(v => wildcardQuery("subNo", v.value.toLowerCase)),
      term.map(wildcardQuery("term", _))
    ).flatten

    val sampleTypeFilter = Seq(q.map(query)).flatten

    val contentQuery = if (objectTypeFilter.nonEmpty || sampleTypeFilter.nonEmpty) {
      val obj =
        if (objectTypeFilter.nonEmpty)
          Some(
            should(
              hasParentQuery(objectType, must(objectTypeFilter), score = true) innerHit
                innerHits(ObjectSearchService.innerHitParentName),
              must(objectTypeFilter)
            )
          )
        else None
      val freeQuery =
        if (sampleTypeFilter.nonEmpty)
          Some(
            should(
              hasParentQuery(objectType, must(sampleTypeFilter), score = true) innerHit
                innerHits(ObjectSearchService.innerHitParentName),
              must(sampleTypeFilter)
            )
          )
        else None

      Some(must(List(obj, freeQuery).flatten))
    } else {
      None
    }

    val isNotDeleted = should(
      hasParentQuery(objectType, termQuery("isDeleted", false), score = false),
      termQuery("isDeleted", false)
    )

    val onlyAllowedObjectsQuery =
      restrictToCollectionAndMuseumQuery(mid, collectionIds).appendMust(
        termQuery("_type" -> objectType)
      )

    val onlyAllowedSamplesQuery =
      must(
        hasParentQuery(
          objectType,
          restrictToCollectionAndMuseumQuery(mid, collectionIds),
          score = true
        ) innerHit innerHits(ObjectSearchService.innerHitParentName)
      ).appendMust(termQuery("_type" -> sampleType))

    val combinedQuery = {
      must(
        should(
          onlyAllowedObjectsQuery,
          onlyAllowedSamplesQuery
        ),
        isNotDeleted
      ).appendMust(contentQuery)
    }
    combinedQuery
  }

}

object ObjectSearchService {
  val innerHitParentName = "musit_object"
}