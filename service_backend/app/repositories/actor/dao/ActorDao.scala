package repositories.actor.dao

import com.google.inject.{Inject, Singleton}
import models.actor.Person
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{ActorId, DatabaseId, MuseumId}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

@Singleton
class ActorDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends ActorTables {

  import profile.api._

  def getByDbId(id: DatabaseId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.id === id).result.headOption)
  }

  def getByActorId(uuid: ActorId): Future[Option[Person]] = {
    val query = actorTable.filter { a =>
      a.applicationId === uuid || a.dpId === uuid
    }
    db.run(query.result.headOption)
  }

  def getNamesForActorIds(
      actorIds: Set[ActorId]
  ): Future[MusitResult[Map[ActorId, String]]] = {
    val query = actorTable.filter { a =>
      (a.applicationId inSet actorIds) || (a.dpId inSet actorIds)
    }.map(a => (a.applicationId, a.dpId, a.fn))

    db.run(query.result).map { nameIds =>
      MusitSuccess(nameIds.flatMap {
        case (appId, ipId, name) =>
          List(appId, ipId).flatten.map(id => id -> name)
      }.toMap)
    }
  }

  def getByName(mid: MuseumId, searchString: String): Future[Seq[Person]] = {
    val likeArg = searchString.toLowerCase
    val query = actorTable.filter { a =>
      (a.fn.toLowerCase like s"%$likeArg%") && a.museumId === mid
    }.sortBy(_.fn)

    db.run(query.result)
  }

  def getByDataportenId(dataportenId: ActorId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.dpId === dataportenId).sortBy(_.fn).result.headOption)
  }

  def listBy(ids: Set[ActorId]): Future[Seq[Person]] = {
    db.run(actorTable.filter { a =>
      (a.applicationId inSet ids) || (a.dpId inSet ids)
    }.sortBy(_.fn).result)
  }

}
