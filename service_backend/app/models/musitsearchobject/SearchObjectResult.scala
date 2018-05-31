package models.musitsearchobject

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.{MuseumId, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}

/** Returned from the database based object search */
case class SearchObjectResult(
    //  id: ObjectId,
    uuid: ObjectUUID,
    museumId: MuseumId,
    museumNo: String,
    subNo: Option[String],
    term: String
//    collection: Option[Collection]
//    document_json: Option[String],
//    updatedDate: DateTime
)
//
object SearchObjectResult {

  implicit val writes: Writes[SearchObjectResult] = Json.writes[SearchObjectResult]

}
