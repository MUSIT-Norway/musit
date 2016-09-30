package no.uio.musit.microservice.event.dao

import no.uio.musit.microservice.event.domain.LocalObject
import no.uio.musit.microservice.event.service.{MoveObject, MovePlace}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.dbio.FutureAction
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by jarle on 22.08.16.
 */

object MovePlaceDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def updateStorageNodeLatestMove(storageNodeId: Int, newEventId: Long, parentNodeId: Int): DBIO[Unit] = {
    /* TODO: When service_storageAdmin and service_event has been merged, activate this code (also need to import StorageNodeTable)
    val q = for {
      l <- StorageNodeTable if l.storageNodeId === storageNodeId
    } yield (l.latestMoveId, l.isPartOf)
    q.update(Some(newEventId), Some(parentNodeId)).map(_ => ())
    */
    DBIO.successful(())
  }

  def executeMove(newEventId: Long, movePlace: MovePlace): DBIO[Unit] = {
    require(movePlace.relatedObjects.length <= 1, "More than one objectId in executeMovePlace.")
    require(movePlace.relatedPlaces.length <= 1, "More than one place in related places.")

    val optPlaceAsObjectAndRelation = movePlace.relatedObjects.headOption

    val optPlaceWithRelation = movePlace.relatedPlaces.headOption //TODO: Needs more elaborate logic here if we specify both from and to later on,
    // now we assume we only have a toPlace relation..
    optPlaceWithRelation match {
      case None => throw new Exception("Missing place to move to for MovePlace event")
      case Some(placeWithRelation) =>
        optPlaceAsObjectAndRelation match {
          case None => throw new Exception("Missing place to move for MovePlace event")
          case Some(placeAsObjectAndRelation) =>
            updateStorageNodeLatestMove(placeAsObjectAndRelation.objectId.toInt, newEventId, placeWithRelation.placeId)
        }
    }
  }
}

