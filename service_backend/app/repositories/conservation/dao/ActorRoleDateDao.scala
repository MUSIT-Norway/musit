package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{ActorId, EventId}
import no.uio.musit.repositories.DbErrorHandlers
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils
import repositories.shared.dao.ColumnTypeMappers

import scala.concurrent.ExecutionContext
@Singleton
class ActorRoleDateDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils
) extends ConservationEventTableProvider
    with ColumnTypeMappers
    with ConservationTables
    with DbErrorHandlers {

  import profile.api._

  private val eventActorRoleDateTable = TableQuery[EventActorAndRoleAndDate]
  private val roleTable               = TableQuery[Role]

  def insertActorRoleAction(
      eventId: EventId,
      roleId: Int,
      actorId: ActorId,
      whatDate: Option[DateTime]
  ): DBIO[Int] = {
    val action = eventActorRoleDateTable += EventActorRoleDate(
      eventId,
      roleId,
      actorId,
      whatDate
    )
    action
  }

  /**
   * an insert action for inserting into table objectEvent
   *
   * @param eventId the eventId
   * @param actorsAndDates a list of actors,roles and dates that relates to the eventId
   * @return a DBIO[Int] Number of rows inserted?
   */
  def insertActorRoleDateAction(
      eventId: EventId,
      actorsAndDates: Seq[ActorRoleDate]
  ): DBIO[Int] = {
    val actions =
      actorsAndDates.map(m => insertActorRoleAction(eventId, m.roleId, m.actorId, m.date))
    DBIO.sequence(actions).map(_.sum)
  }

  def deleteActorRoleDateAction(eventId: EventId): DBIO[Int] = {
    val q      = eventActorRoleDateTable.filter(oe => oe.eventId === eventId)
    val action = q.delete
    action
  }

  def updateActorRoleDateAction(
      eventId: EventId,
      actorAndDates: Seq[ActorRoleDate]
  ): DBIO[Int] = {
    for {
      deleted  <- deleteActorRoleDateAction(eventId)
      inserted <- insertActorRoleDateAction(eventId, actorAndDates)
    } yield inserted
  }

  def getEventActorRoleDates(eventId: EventId): FutureMusitResult[Seq[ActorRoleDate]] = {
    val action =
      eventActorRoleDateTable
        .filter(_.eventId === eventId)
        .sortBy(_.roleId)
        .map(ard => (ard.roleId, ard.actorId, ard.actorDate))
        .result
    daoUtils
      .dbRun(action, s"getEventActorRoleDatesAction failed for eventId $eventId")
      .map(_.map(m => ActorRoleDate(m._1, m._2, m._3)))
  }

  def getRoleList: FutureMusitResult[Seq[EventRole]] = {
    daoUtils.dbRun(roleTable.result, "getRoleList failed")
  }

  private class EventActorAndRoleAndDate(tag: Tag)
      extends Table[EventActorRoleDate](
        tag,
        Some(SchemaName),
        EventActorsRolesTableName
      ) {
    val eventId   = column[EventId]("EVENT_ID")
    val roleId    = column[Int]("ROLE_ID")
    val actorId   = column[ActorId]("ACTOR_ID")
    val actorDate = column[Option[DateTime]]("ACTOR_ROLE_DATE")

    val create = (
        eventId: EventId,
        roleId: Int,
        actorId: ActorId,
        actorDate: Option[DateTime]
    ) =>
      EventActorRoleDate(
        eventId = eventId,
        roleId = roleId,
        actorId = actorId,
        actorDate = actorDate
    )

    val destroy = (eard: EventActorRoleDate) =>
      Some(
        (
          eard.eventId,
          eard.roleId,
          eard.actorId,
          eard.actorDate
        )
    )

    // scalastyle:off method.name
    def * = (eventId, roleId, actorId, actorDate) <> (create.tupled, destroy)

    // scalastyle:on method.name

  }

  private class Role(tag: Tag)
      extends Table[EventRole](
        tag,
        Some(SchemaName),
        RolesTableName
      ) {
    val roleId  = column[Int]("ROLE_ID")
    val noRole  = column[String]("NO_ROLE")
    val enRole  = column[String]("EN_ROLE")
    val roleFor = column[String]("ROLE_FOR")

    val create = (
        roleId: Int,
        noRole: String,
        enRole: String,
        roleFor: String
    ) =>
      EventRole(
        roleId = roleId,
        noRole = noRole,
        enRole = enRole,
        roleFor = roleFor
    )

    val destroy = (er: EventRole) =>
      Some(
        (
          er.roleId,
          er.noRole,
          er.enRole,
          er.roleFor
        )
    )

    // scalastyle:off method.name
    def * = (roleId, noRole, enRole, roleFor) <> (create.tupled, destroy)

    // scalastyle:on method.name

  }

}
