package models.elasticsearch

import models.conservation.events._
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._

object Constants {
  val collectionUuid = "collectionUuid"
  val museumId       = "museumId"
  val eventTypeEn    = "eventTypeEn"
  val eventTypeNo    = "eventTypeNo"
  val actors         = "actors"
}

case class ActorRoleDateName(
    actorId: ActorId,
    name: String,
    role_en: String,
    role_no: String,
    date: Option[DateTime]
)

object ActorRoleDateName extends WithDateTimeFormatters {
  implicit val writes: Writes[ActorRoleDateName] = Json.writes[ActorRoleDateName]
}

/* This is the object which gets written to ES. At the moment we just embed the event directly.
 (But for ConservationProcesses we ignore the children and assume they have already been removed)
 */

case class ConservationSearch(
    museumId: MuseumId,
    collectionUuid: Option[CollectionUUID],
    event: ConservationModuleEvent,
    eventType: ConservationType,
    actors: Option[Seq[ActorRoleDateName]]
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

  def translateEventRole(
      actorNames: ActorNames,
      person: Option[ActorId],
      date: Option[DateTime],
      role_en: String,
      role_no: String
  ) = {
    for {
      actorId   <- person
      actorName <- actorNames.nameFor(actorId)
    } yield ActorRoleDateName(actorId, actorName, role_en, role_no, date)

  }

  def translateActorWithRole(
      actorRoleDate: ActorRoleDate,
      actorNames: ActorNames,
      allEventRoles: Seq[EventRole]
  ) = {
    val roleId       = actorRoleDate.roleId
    val optEventRole = allEventRoles.find(_.roleId == roleId)
    optEventRole match {
      case Some(eventRole) =>
        translateEventRole(
          actorNames,
          Some(actorRoleDate.actorId),
          actorRoleDate.date,
          eventRole.enRole,
          eventRole.noRole
        )
      case None =>
        ConservationSearch.logger.error(s"Unknown event role id: $roleId")
        None
    }
  }

  def withActorNames(
      actorNames: ActorNames,
      allEventRoles: Seq[EventRole]
  ): ConservationSearch = {
    val actorsAndRolesSeq = event.actorsAndRoles
      .map(
        _.map(
          translateActorWithRole(_, actorNames, allEventRoles)
        ).flatten
      )
      .getOrElse(Seq.empty)

    val actorsSeq = Seq(
      translateEventRole(
        actorNames,
        event.registeredBy,
        event.registeredDate,
        "Registered by",
        "Registrert av"
      ),
      translateEventRole(
        actorNames,
        event.updatedBy,
        event.updatedDate,
        "Updated by",
        "Oppdatert av"
      )
    ).flatten.union(actorsAndRolesSeq)

    val res = if (actorsSeq.isEmpty) None else Some(actorsSeq)

    copy(actors = res)
  }
}

object ConservationSearch {
  def apply(
      museumId: MuseumId,
      collectionUuid: Option[CollectionUUID],
      event: ConservationModuleEvent,
      eventType: ConservationType
  ): ConservationSearch = {
    ConservationSearch(museumId, collectionUuid, event, eventType, None)
  }

  val logger = Logger(classOf[ConservationSearch])

  implicit val readsEvent = models.conservation.events.ConservationModuleEvent.reads

  /* The reason I did a custom writer instead of a fully automatic one is to keep the object stored in ES as flat/efficient as possible.
  (But it can be argued that this isn't necessary, so please feel free to rewrote this to use a standard writer.)
   */

  implicit val conservationSearchWrites = new Writes[ConservationSearch] {
    def writes(conservationSearch: ConservationSearch) = {
      val eventType = conservationSearch.eventType
      val jsObj = Json.obj(
        Constants.museumId       -> conservationSearch.museumId,
        Constants.collectionUuid -> conservationSearch.collectionUuid,
        Constants.eventTypeEn    -> eventType.enName,
        Constants.eventTypeNo    -> eventType.noName,
        Constants.actors         -> conservationSearch.actors
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
