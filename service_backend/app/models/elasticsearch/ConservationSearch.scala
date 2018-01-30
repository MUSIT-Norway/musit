package models.elasticsearch

import models.conservation.events.{ConservationModuleEvent, ConservationType}
import no.uio.musit.models._
import play.api.libs.json._

/* At the moment we just store the whole event (as JSON). (But for ConservationProcesses, we ignore the children)
 */
object Constants {
  val collectionUuid = "collectionUuid"
  val museumId       = "museumId"
  val eventTypeEn    = "eventTypeEn"
  val eventTypeNo    = "eventTypeNo"
  val actors         = "actors"
}

case class ConservationSearch(
    museumId: MuseumId,
    collectionUuid: Option[CollectionUUID],
    event: ConservationModuleEvent,
    eventType: ConservationType,
    actorNames: Option[Seq[String]]
) {

  def collectMentionedActorIds() = {
    val innerActors = event.actorsAndRoles.map(_.map(_.actorId))
    val outerActors = Set(event.registeredBy, event.updatedBy).flatten
    val res = innerActors match {
      case Some(actorSeq) => actorSeq.toSet.union(outerActors)
      case None           => outerActors
    }
    res
  }

  def withActorNames(actorNames: ActorNames) = {
    val names =
      collectMentionedActorIds().map(actorId => actorNames.nameFor(actorId)).flatten
    val optSeqNames = if (names.isEmpty) None else (Some(names.toSeq))

    //println(s"ActorIds: ${collectMentionedActorIds()}")
    //println(s"Actors: $optSeqNames")

    this.copy(actorNames = optSeqNames)
  }
}

object ConservationSearch {
  implicit val readsEvent = models.conservation.events.ConservationModuleEvent.reads

  /* The reason I did a custom writer instead of a fully automatic one is to keep the object stored in ES as flat/efficient as possible.
  (Also, when I started writing it it only had the museumId and collectionUuid in addition to the event.)
   */

  implicit val conservationSearchWrites = new Writes[ConservationSearch] {
    def writes(conservationSearch: ConservationSearch) = {
      val eventType = conservationSearch.eventType
      val jsObj = Json.obj(
        Constants.museumId       -> conservationSearch.museumId,
        Constants.collectionUuid -> conservationSearch.collectionUuid,
        Constants.eventTypeEn    -> eventType.enName,
        Constants.eventTypeNo    -> eventType.noName,
        Constants.actors         -> conservationSearch.actorNames
      )

      val jsObj2 = Json.toJson(conservationSearch.event).as[JsObject]
      jsObj ++ jsObj2
    }
  }
  /* At the moment we don't need to read in ConservationSearch objects. This code can likely be deleted.
  implicit val conservationSearchReads: Reads[ConservationSearch] =
    new Reads[ConservationSearch] {
      def reads(json: JsValue): JsResult[ConservationSearch] = {
        for {
          museumId          <- (json \ Constants.museumId).validate[MuseumId]
          collectionUuid    <- (json \ Constants.collectionUuid).validateOpt[CollectionUUID]
          conservationEvent <- json.validate[ConservationModuleEvent]
          ..handle eventTypeEn and eventTypeNo and actors..
        } yield ConservationSearch(museumId, collectionUuid, conservationEvent, None)
      }
    }
 */
}
