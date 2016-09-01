package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.LocalObject
import no.uio.musit.microservice.event.service.MoveObject
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.dbio.FutureAction
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by jarle on 22.08.16.
 */

object MoveObjectDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val LocalObjectsTable = TableQuery[LocalObjectsTable]

  def insertLocalObject(newObjectId: Long): DBIO[Unit] = {
    val res = LocalObjectsTable.map(lot => lot.objectId) += (newObjectId)
    res.map(_ => ())
  }

  def updateLocalObjectLatestMove(localObjectId: Long, newEventId: Long, currentLocationId: Int): DBIO[Unit] = {
    val q = for {
      l <- LocalObjectsTable if l.objectId === localObjectId
    } yield (l.latestMoveId, l.currentLocationId)
    q.update(Some(newEventId), Some(currentLocationId)).map(_ => ())
  }

  def maybeInsertLocalObject(localObjectId: Long): DBIO[Unit] = {
    val futOptRes = FutureAction(db.run(LocalObjectsTable.filter(museumObject => museumObject.objectId === localObjectId).result.headOption))
    futOptRes.flatMap { res =>
      res match {
        case None => insertLocalObject(localObjectId)
        case _ =>
          DBIO.successful(())
      }
    }
  }

  def executeMove(newEventId: Long, moveObject: MoveObject): DBIO[Unit] = {
    require(moveObject.relatedObjects.length <= 1, "More than one objectId in executeMove.")
    require(moveObject.relatedPlaces.length <= 1, "More than one place in related places.")

    val localObjectAndRelation = moveObject.relatedObjects.headOption

    val optPlaceWithRelation = moveObject.relatedPlaces.headOption //TODO: Needs more elaborate logic here if we specify both from and to later on,
    // now we assume we only have a toPlace relation..
    optPlaceWithRelation match {

      case None => throw new Exception("Missing place for move event")
      case Some(placeWithRelation) =>

        localObjectAndRelation match {

          case None => throw new Exception("Missing object to move")
          case Some(localObjectAndRelation) =>
            maybeInsertLocalObject(localObjectAndRelation.objectId)
              .andThen(updateLocalObjectLatestMove(localObjectAndRelation.objectId, newEventId, placeWithRelation.placeId))
        }
    }
  }

  private class LocalObjectsTable(tag: Tag) extends Table[LocalObject](tag, Some("MUSARK_EVENT"), "LOCAL_OBJECT") {
    def * = (objectId, latestMoveId, currentLocationId) <> (create.tupled, destroy) // scalastyle:ignore

    val objectId = column[Long]("OBJECT_ID")
    val latestMoveId = column[Option[Long]]("LATEST_MOVE_ID")
    val currentLocationId = column[Option[Int]]("CURRENT_LOCATION_ID")

    def create = (objectId: Long, latestMoveId: Option[Long], currentLocationId: Option[Int]) =>
      LocalObject(
        objectId,
        latestMoveId,
        currentLocationId
      )

    def destroy(localObject: LocalObject) = Some(localObject.objectId, localObject.latestMoveId, localObject.currentLocationId)
  }

}

