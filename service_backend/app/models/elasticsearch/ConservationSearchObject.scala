package models.elasticsearch

import com.sksamuel.elastic4s.Indexable
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
  val isDeleted      = "isDeleted"
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

/**Represents a conservation object as indexed into ElasticSearch.
 * The name is a bit weird/long, but on the other hand, since we currently index both existing and deleted objects, it's perhaps
 * ok with this long name as it is easy to forget that we index both existing and deleted objects.
 * We may perhaps remove the need to index deleted objects in the future, but a mechanism to switch the status from existing to deleted
 * will still be needed.
 *
 */
trait DeletedOrExistingConservationSearchObject {

  def collectMentionedActorIds(): Set[ActorId]

  def withActorNames(
      actorNames: ActorNames,
      allEventRoles: Seq[EventRole]
  ): DeletedOrExistingConservationSearchObject

  def eventId: EventId

  /**Returns the json-representation (which gets written to ES) of this object.
   * Could/should probably use some magic to get an implicit evidence of this object as an Indexable,
   * but I'm not good enough at Scala to try to make that work, so I did it in this more "manual"/explicit way instead.

   */
  def toJson(): String
}

/* This is the object which gets written to ES. At the moment we just embed the event directly.
 (But for ConservationProcesses we ignore the children and assume they have already been removed)
 */

/*For conservation events which has been deleted */
case class DeletedConservationSearchObject(
    eventId: EventId,
    museumId: MuseumId,
    collectionUuid: Option[CollectionUUID]
) extends DeletedOrExistingConservationSearchObject {

  def collectMentionedActorIds(): Set[ActorId] = Set.empty

  def withActorNames(
      actorNames: ActorNames,
      allEventRoles: Seq[EventRole]
  ): DeletedOrExistingConservationSearchObject = this
  DeletedConservationSearchObject

  def toJson =
    DeletedConservationSearchObject.json(
      this.asInstanceOf[DeletedConservationSearchObject]
    )

}

/*For non-deleted conservation events*/
case class ExistingConservationSearchObject(
    museumId: MuseumId,
    collectionUuid: Option[CollectionUUID],
    event: ConservationModuleEvent,
    eventType: ConservationType,
    actors: Option[Seq[ActorRoleDateName]]
) extends DeletedOrExistingConservationSearchObject {

  def eventId: EventId = event.id.get

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
        ExistingConservationSearchObject.logger.error(s"Unknown event role id: $roleId")
        None
    }
  }

  def withActorNames(
      actorNames: ActorNames,
      allEventRoles: Seq[EventRole]
  ): DeletedOrExistingConservationSearchObject = {
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

  def toJson =
    ExistingConservationSearchObject.json(
      this.asInstanceOf[ExistingConservationSearchObject]
    )

}

/*For conservation events which has been deleted */
object DeletedConservationSearchObject {

  val logger = Logger(classOf[DeletedConservationSearchObject])

  implicit val deletedConservationSearchWrites =
    new Writes[DeletedConservationSearchObject] {
      def writes(deletedConservationSearch: DeletedConservationSearchObject) = {
        val jsObj = Json.obj(
          Constants.museumId       -> deletedConservationSearch.museumId,
          Constants.collectionUuid -> deletedConservationSearch.collectionUuid,
          "id"                     -> deletedConservationSearch.eventId,
          Constants.isDeleted      -> true
        )
        jsObj
      }
    }

  def json(t: DeletedConservationSearchObject): String = {
    //Json.stringify(Json.toJson(t)(deletedConservationSearchWrites))
    Json.stringify(this.deletedConservationSearchWrites.writes(t))
  }
}

object ExistingConservationSearchObject {
  def apply(
      museumId: MuseumId,
      collectionUuid: Option[CollectionUUID],
      event: ConservationModuleEvent,
      eventType: ConservationType
  ): ExistingConservationSearchObject = {
    ExistingConservationSearchObject(museumId, collectionUuid, event, eventType, None)
  }

  val logger = Logger(classOf[ExistingConservationSearchObject])

  implicit val readsEvent = models.conservation.events.ConservationModuleEvent.reads

  /* The reason I did a custom writer instead of a fully automatic one is to keep the object stored in ES as flat/efficient as possible.
  (But it can be argued that this isn't necessary, so please feel free to rewrote this to use a standard writer.)
   */

  implicit val conservationSearchWrites = new Writes[ExistingConservationSearchObject] {
    def writes(conservationSearch: ExistingConservationSearchObject) = {
      val eventType = conservationSearch.eventType
      val jsObj = Json.obj(
        Constants.museumId       -> conservationSearch.museumId,
        Constants.collectionUuid -> conservationSearch.collectionUuid,
        Constants.eventTypeEn    -> eventType.enName,
        Constants.eventTypeNo    -> eventType.noName,
        Constants.actors         -> conservationSearch.actors,
        Constants.isDeleted      -> false
      )

      val jsObj2 = Json.toJson(conservationSearch.event).as[JsObject]
      jsObj ++ jsObj2
    }
  }

  def json(t: ExistingConservationSearchObject): String = {
    //Json.stringify(Json.toJson(t)(deletedConservationSearchWrites))
    Json.stringify(this.conservationSearchWrites.writes(t))
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
