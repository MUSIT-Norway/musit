package repositories.storage.old_dao.event

import models.storage.event.dto.EventRelation
import no.uio.musit.models.EventId

/**
 * TODO: What am I for?
 */
object EventRelationTypes {

  /**
   * TODO: What am I and what is my purpose?
   */
  case class PartialEventRelation(idFrom: Long, relation: EventRelation) {
    def toFullLink(idTo: Long) = FullEventRelation(idFrom, relation, idTo)
  }

  /**
   * TODO: What am I and what is my purpose?
   */
  case class FullEventRelation(
      idFrom: Long,
      relation: EventRelation,
      idTo: Long
  ) {

    /**
     * TODO: What do I do?
     */
    def normalizedDirection = {
      if (relation.isNormalized) this
      else FullEventRelation(idTo, relation.getNormalizedDirection, idFrom)
    }

    /**
     * TODO: What do I do?
     */
    def toEventLinkDto = EventRelationDto(idFrom, relation.id, idTo)

    /**
     * TODO: What do I do?
     */
    def toNormalizedEventLinkDto = normalizedDirection.toEventLinkDto

  }

  /**
   * TODO: What am I and what is my purpose?
   */
  case class EventRelationDto(idFrom: EventId, relationId: Int, idTo: EventId)

}
