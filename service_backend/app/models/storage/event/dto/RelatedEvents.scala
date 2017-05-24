package models.storage.event.dto

/**
 * Events related (via relation) to a given event.
 */
case class RelatedEvents(relation: EventRelation, events: Seq[EventDto])
