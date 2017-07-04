package models.analysis.events

import models.analysis.events.AnalysisResults.AnalysisResult
import no.uio.musit.models.{EventId, ObjectUUID}
import play.api.libs.json.{Json, Reads}

/**
 * An aggregate result type that includes an reference to the objectId and
 * eventId the result should be associated with. To ensure correctness before
 * saving the result, it is necessary to validate the objectId and eventId
 * against the [[Analysis]] associated. This should be done in the service layer.
 *
 * @param objectId The [[ObjectUUID]] of the object to add the result to.
 * @param eventId The [[EventId]] that should reference the above object.
 * @param result The [[AnalysisResult]] to set
 */
case class ResultForObjectEvent(
    objectId: ObjectUUID,
    eventId: EventId,
    result: AnalysisResult
)

object ResultForObjectEvent {

  implicit val reads: Reads[ResultForObjectEvent] = Json.reads[ResultForObjectEvent]

}

/**
 * Type specifically intended for importing a batch of result for a specific
 * [[AnalysisCollection]] and it's [[Analysis]] children.
 *
 * @param collectionResult The main result for the [[AnalysisCollection]] event.
 * @param objectResults The result for objects associated with the collection.
 *                      These are saved as [[Analysis]] events as children of the
 *                      above collection.
 */
case class AnalysisResultImport(
    collectionResult: AnalysisResult,
    objectResults: Seq[ResultForObjectEvent]
)

object AnalysisResultImport {

  implicit val reads: Reads[AnalysisResultImport] = Json.reads[AnalysisResultImport]

}
