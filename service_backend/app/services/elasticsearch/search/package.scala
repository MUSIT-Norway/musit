package services.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{matchQuery, must, should, termQuery}
import no.uio.musit.models.{MuseumCollection, MuseumId}
import no.uio.musit.security.AuthenticatedUser

package object search {

  def restrictToCollectionAndMuseumQuery(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser) = {
    if (currUsr.hasGodMode)
      must(termQuery("museumId", mid.underlying))
    else
      must(
        should(
          collectionIds.map { c =>
            matchQuery("collection.uuid", c.uuid.underlying.toString)
          }
        ),
        matchQuery("museumId", mid.underlying)
      )
  }

}
