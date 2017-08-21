package models.elasticsearch

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.events.AnalysisExtras.ExtraAttributes
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import models.elasticsearch.Actors.{ActorSearch, ActorSearchStamp}
import no.uio.musit.formatters.DateTimeFormatters._
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json, Writes}

case class ActorNames(m: Map[ActorId, String]) {
  def nameFor(id: ActorId): Option[String] = m.get(id)
}

object ActorNames {
  def apply(s: Set[(ActorId, String)]): ActorNames = ActorNames(s.toMap)
}

case class AnalysisSearch(
    id: EventId,
    analysisTypeId: AnalysisTypeId, //todo inline values to make them searchable
    doneBy: Option[ActorSearchStamp],
    registeredBy: Option[ActorSearchStamp],
    responsible: Option[ActorSearch],
    administrator: Option[ActorSearch],
    updatedBy: Option[ActorSearchStamp],
    completedBy: Option[ActorSearchStamp],
    objectId: Option[ObjectUUID],
    objectType: Option[ObjectType],
    partOf: Option[EventId],
    note: Option[String],
    extraAttributes: Option[ExtraAttributes],
    result: Option[AnalysisResultSearch]
) extends Searchable {
  override val docId       = id.underlying.toString
  override val docParentId = partOf.map(_.underlying.toString)
}

object AnalysisSearch {
  implicit val writes: Writes[AnalysisSearch] = Json.writes[AnalysisSearch]

  def apply(a: Analysis, actorNames: ActorNames): AnalysisSearch =
    AnalysisSearch(
      id = a.id.get,
      analysisTypeId = a.analysisTypeId,
      doneBy = ActorSearchStamp(a.doneBy, a.doneDate, actorNames),
      registeredBy = ActorSearchStamp(a.registeredBy, a.registeredDate, actorNames),
      responsible = a.responsible.map(id => ActorSearch(id, actorNames.nameFor(id))),
      administrator = a.administrator.map(id => ActorSearch(id, actorNames.nameFor(id))),
      updatedBy = ActorSearchStamp(a.updatedBy, a.updatedDate, actorNames),
      completedBy = ActorSearchStamp(a.completedBy, a.completedDate, actorNames),
      objectId = a.affectedThing,
      objectType = a.affectedType,
      partOf = a.partOf,
      note = a.note,
      extraAttributes = a.extraAttributes,
      result = a.result.map(AnalysisResultSearch.apply)
    )
}

case class AnalysisCollectionSearch(
    id: EventId,
    analysisTypeId: AnalysisTypeId, //todo inline values to make them searchable
    doneBy: Option[ActorSearchStamp],
    registeredBy: Option[ActorSearchStamp],
    responsible: Option[ActorSearch],
    administrator: Option[ActorSearch],
    updatedBy: Option[ActorSearchStamp],
    completedBy: Option[ActorSearchStamp],
    note: Option[String],
    extraAttributes: Option[ExtraAttributes],
    result: Option[AnalysisResultSearch],
    restriction: Option[Restriction],
    reason: Option[String],
    status: Option[AnalysisStatus], //todo inline values to make them searchable
    caseNumbers: Option[CaseNumbers],
    orgId: Option[OrgId]
) extends Searchable {
  override val docId                       = id.underlying.toString
  override val docParentId: Option[String] = None
}

object AnalysisCollectionSearch {
  implicit val writes: Writes[AnalysisCollectionSearch] =
    Json.writes[AnalysisCollectionSearch]

  def apply(a: AnalysisCollection, actorNames: ActorNames): AnalysisCollectionSearch =
    AnalysisCollectionSearch(
      id = a.id.get,
      analysisTypeId = a.analysisTypeId,
      doneBy = ActorSearchStamp(a.doneBy, a.doneDate, actorNames),
      registeredBy = ActorSearchStamp(a.registeredBy, a.registeredDate, actorNames),
      responsible = a.responsible.map(id => ActorSearch(id, actorNames.nameFor(id))),
      administrator = a.administrator.map(id => ActorSearch(id, actorNames.nameFor(id))),
      updatedBy = ActorSearchStamp(a.updatedBy, a.updatedDate, actorNames),
      completedBy = ActorSearchStamp(a.completedBy, a.completedDate, actorNames),
      note = a.note,
      extraAttributes = a.extraAttributes,
      result = a.result.map(AnalysisResultSearch.apply),
      restriction = None,
      reason = a.reason,
      status = a.status,
      caseNumbers = a.caseNumbers,
      orgId = a.orgId
    )
}

case class SampleCreatedSearch(
    id: EventId,
    doneBy: Option[ActorSearchStamp],
    registeredBy: Option[ActorSearchStamp],
    objectId: Option[ObjectUUID],
    sampleObjectId: Option[ObjectUUID],
    externalLinks: Option[Seq[String]]
) extends Searchable {
  override val docId       = id.underlying.toString
  override val docParentId = None
}

object SampleCreatedSearch {
  implicit val writes: Writes[SampleCreatedSearch] = Json.writes[SampleCreatedSearch]

  def apply(s: SampleCreated, actorNames: ActorNames): SampleCreatedSearch =
    SampleCreatedSearch(
      id = s.id.get,
      doneBy = ActorSearchStamp(s.doneBy, s.doneDate, actorNames),
      registeredBy = ActorSearchStamp(s.registeredBy, s.registeredDate, actorNames),
      objectId = s.affectedThing,
      sampleObjectId = s.sampleObjectId,
      externalLinks = s.externalLinks
    )
}

case class AnalysisResultSearch(origin: AnalysisResult, registeredByName: Option[String])

object AnalysisResultSearch {
  implicit val writes: Writes[AnalysisResultSearch] = Writes[AnalysisResultSearch] {
    result =>
      Json.toJson[AnalysisResult](result.origin).as[JsObject] ++ Json.obj(
        "registeredByName" -> result.registeredByName
      )
  }

  def apply(result: AnalysisResult): AnalysisResultSearch =
    AnalysisResultSearch(result, None)
}

case class RestrictionSearch(
    requester: ActorSearch,
    expirationDate: DateTime,
    reason: String,
    caseNumbers: Option[CaseNumbers] = None,
    registeredStamp: Option[ActorSearchStamp] = None,
    cancelledStamp: Option[ActorSearchStamp] = None,
    cancelledReason: Option[String] = None
)

object RestrictionSearch {
  implicit val writes = Json.writes[RestrictionSearch]
}

sealed trait AnalysisModuleEventSearch

object AnalysisModuleEventSearch {
  def apply(mid: MuseumId, event: AnalysisModuleEvent): AnalysisModuleEventSearch =
    event match {
      case a: Analysis            => AnalysisSearchType(mid, a)
      case ac: AnalysisCollection => AnalysisCollectionSearchType(mid, ac)
      case ar: SampleCreated      => SampleCreatedEventSearchType(mid, ar)
    }
}

final case class AnalysisSearchType(
    museumId: MuseumId,
    event: Analysis
) extends AnalysisModuleEventSearch

final case class AnalysisCollectionSearchType(
    museumId: MuseumId,
    event: AnalysisCollection
) extends AnalysisModuleEventSearch

final case class SampleCreatedEventSearchType(
    museumId: MuseumId,
    event: SampleCreated
) extends AnalysisModuleEventSearch
