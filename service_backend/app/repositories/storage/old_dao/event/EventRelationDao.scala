package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.BaseEventDto
import no.uio.musit.models.EventId
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables
import repositories.storage.old_dao.event.EventRelationTypes.{
  EventRelationDto,
  FullEventRelation
}

import scala.concurrent.Future

@Singleton
class EventRelationDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  private val logger = Logger(classOf[EventRelationDao])

  import profile.api._

  def insertRelationAction(relation: FullEventRelation): DBIO[Int] = {
    insertEventRelationDtoAction(relation.toNormalizedEventLinkDto)
  }

  def insertEventRelationDtoAction(relation: EventRelationDto): DBIO[Int] = {
    logger.debug(
      s"inserting relation with relationId: ${relation.relationId}" +
        s" from: ${relation.idFrom} to: ${relation.idTo}"
    )

    eventRelTable += relation
  }

  def getRelatedEvents(parentId: EventId): Future[Seq[(Int, BaseEventDto)]] = {
    val relevantRelations = eventRelTable.filter(evt => evt.fromId === parentId)

    logger.debug(s"gets relatedEventDtos for parentId: $parentId")

    val action = eventBaseTable.join(relevantRelations).on(_.id === _.toId)

    val query = for {
      (eventBaseTable, relationTable) <- action
    } yield (relationTable.relationId, eventBaseTable)

    db.run(query.result)

  }

}
