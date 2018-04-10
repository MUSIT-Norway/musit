package services.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{matchQuery, must, should, termQuery}
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser

package object search {

  def restrictToCollectionAndMuseumQuery(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      collectionUuidFieldName: String = "collection.uuid"
  )(implicit currUsr: AuthenticatedUser): BoolQueryDefinition = {
    if (currUsr.hasGodMode)
      must(termQuery("museumId", mid.underlying))
    else
      must(
        should(
          collectionIds.map { c =>
            termQuery(collectionUuidFieldName, c.uuid.underlying.toString)
          }
        ),
        termQuery("museumId", mid.underlying)
      )
  }

}
