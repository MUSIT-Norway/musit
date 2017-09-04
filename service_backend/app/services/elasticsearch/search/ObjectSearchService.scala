package services.elasticsearch.search

import com.google.inject.Inject
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl.{termQuery, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchResponse
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser
import services.elasticsearch.elastic4s.{MusitESResponse, MusitSearchHttpExecutable}
import services.elasticsearch.index.things.{indexAlias, objectType, sampleType}

import scala.concurrent.{ExecutionContext, Future}

class ObjectSearchService @Inject()(implicit client: HttpClient, ex: ExecutionContext) {

  def restrictedObjectSearch(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      // query stuff
      museumNo: Option[String],
      subNo: Option[String],
      term: Option[String],
      queryStr: Option[String]
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitESResponse[SearchResponse]] = {
    val qry = createSearchQuery(mid, collectionIds, museumNo, subNo, term, queryStr)

    client.execute(
      search(IndexAndTypes(indexAlias, Seq(objectType, sampleType))) query qry
    )(MusitSearchHttpExecutable.musitSearchHttpExecutable)
  }

  private def createSearchQuery(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      museumNo: Option[String],
      subNo: Option[String],
      term: Option[String],
      q: Option[String]
  )(implicit currUsr: AuthenticatedUser) = {
    val objectTypeFilter = Seq(
      museumNo.map(v => wildcardQuery("museumNo", v.toLowerCase)),
      subNo.map(v => wildcardQuery("subNo", v.toLowerCase)),
      term.map(wildcardQuery("term", _))
    ).flatten

    val sampleTypeFilter = Seq(q.map(query)).flatten

    val contentQuery = if (objectTypeFilter.nonEmpty || sampleTypeFilter.nonEmpty) {
      val obj =
        if (objectTypeFilter.nonEmpty)
          Some(
            should(
              hasParentQuery(objectType, must(objectTypeFilter), score = true) innerHit
                innerHits("parent objects"),
              must(objectTypeFilter)
            )
          )
        else None
      val freeQuery =
        if (sampleTypeFilter.nonEmpty)
          Some(
            should(
              hasParentQuery(objectType, must(sampleTypeFilter), score = true) innerHit
                innerHits("parent objects"),
              must(sampleTypeFilter)
            )
          )
        else None

      Some(should(List(obj, freeQuery).flatten))
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
        ) innerHit innerHits("parent objects")
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
