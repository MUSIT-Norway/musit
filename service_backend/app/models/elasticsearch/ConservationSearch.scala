package models.elasticsearch

import java.util.UUID

import models.conservation.events.{
  ActorRoleDate,
  ConservationEvent,
  ConservationModuleEvent
}
import models.elasticsearch.Actors.{ActorSearch, ActorSearchStamp}
import no.uio.musit.formatters.DateTimeFormatters._
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._
import models.conservation.events.ConservationEvent._

/* At the moment we don't have a custom/specific ES-type for the conservationEvents, we just store the whole event (as JSON).
If that turns out to be suboptimal in the future, then we make a custom ConservationSearch type containing
what we want from the (sub)events.
 */
object Constants {
  val collectionUuid = "collectionUuid"
  val museumId       = "museumId"
}

case class ConservationSearch(
    museumId: MuseumId,
    collectionUuid: Option[CollectionUUID],
    event: ConservationModuleEvent
)

object ConservationSearch {
//  type ConservationSearch = ConservationEvent
  implicit val readsEvent = models.conservation.events.ConservationModuleEvent.reads

  implicit val conservationSearchWrites = new Writes[ConservationSearch] {
    def writes(conservationSearch: ConservationSearch) = {
      val jsObj = Json.obj(
        Constants.museumId       -> conservationSearch.museumId,
        Constants.collectionUuid -> conservationSearch.collectionUuid
      )

      val jsObj2 = Json.toJson(conservationSearch.event).as[JsObject]
      jsObj ++ jsObj2
    }
  }

  implicit val conservationSearchReads: Reads[ConservationSearch] =
    new Reads[ConservationSearch] {
      def reads(json: JsValue): JsResult[ConservationSearch] = {
        for {
          museumId          <- (json \ Constants.museumId).validate[MuseumId]
          collectionUuid    <- (json \ Constants.collectionUuid).validateOpt[CollectionUUID]
          conservationEvent <- json.validate[ConservationModuleEvent]
        } yield ConservationSearch(museumId, collectionUuid, conservationEvent)
      }
    }
}

/*
case class ConservationSearch(
    id: EventId,
    museumId: Option[MuseumId],
    collection: Option[CollectionSearch],
    caseNumber: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]]
//val affectedThings: Option[Seq[ObjectUUID]]
) extends Searchable {
  override val docId       = id.underlying.toString
  override val docParentId = None
}

object ConservationSearch {
  implicit val writes: Writes[ConservationSearch] = Json.writes[ConservationSearch]

  def apply(
      e: ConservationEvent,
      actorNames: ActorNames,
      midAndColl: Option[(MuseumId, MuseumCollections.Collection)]
  ): ConservationSearch =
    ConservationSearch(
      id = e.id.get,
      museumId = midAndColl.map(_._1),
      collection = midAndColl.map(mc => CollectionSearch(mc._2)),
      caseNumber = e.caseNumber,
      actorsAndRoles = None
    )
}
 */
