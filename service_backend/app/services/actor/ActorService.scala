package services.actor

import com.google.inject.Inject
import models.actor.Person
import no.uio.musit.models.{ActorId, MuseumId}
import no.uio.musit.security.UserInfo
import no.uio.musit.service.MusitSearch
import play.api.Logger
import repositories.actor.dao.{ActorDao, UserInfoDao}

import scala.concurrent.{ExecutionContext, Future}

class ActorService @Inject()(
    implicit
    val ec: ExecutionContext,
    val actorDao: ActorDao,
    val usrInfDao: UserInfoDao
) {

  val logger = Logger(classOf[ActorService])

  /**
   * Find the Person that is identified by the given ActorId
   *
   * @param id ActorId
   * @return An option of Person
   */
  def findByActorId(id: ActorId): Future[Option[Person]] = {
    val userInfoPerson = usrInfDao.getById(id)
    val actor          = actorDao.getByActorId(id)

    for {
      uip <- userInfoPerson
      act <- actor
    } yield {
      // We prefer the result from UserInfo over the one in Actor if the
      // ID is registered as an active user of the system.
      uip.map(Person.fromUserInfo).orElse(act)
    }
  }

  /**
   * Find Person details for the given set of ActorIds.
   *
   * @param ids Set[ActorId]
   * @return A collection of Person objects.
   */
  def findDetails(ids: Set[ActorId]): Future[Seq[Person]] = {
    val users  = usrInfDao.listBy(ids)
    val actors = actorDao.listBy(ids)

    for {
      u <- users
      a <- actors
    } yield merge(u, a)
  }

  /**
   * Find the actors/users that match the given search criteria.
   *
   * @param search MusitSearch
   * @return A collection of Person objects.
   */
  def findByName(mid: MuseumId, search: MusitSearch): Future[Seq[Person]] = {
    val searchString = search.searchStrings.reduce(_ + " " + _)
    val users        = usrInfDao.getByName(searchString)
    val actors       = actorDao.getByName(mid, searchString)

    for {
      u <- users
      a <- actors
    } yield merge(u, a)
  }

  private[services] def merge(users: Seq[UserInfo], actors: Seq[Person]): Seq[Person] = {
    def duplicateFilter(p: Person) = users.exists { u =>
      p.dataportenUser.exists(prefix => u.feideUser.exists(_.startsWith(prefix)))
    }

    // Find actors that exist in both the users and actor lists
    val dupes = actors.filter(duplicateFilter)
    // Remove the actors that exist in both lists
    val nodup = actors.filterNot(duplicateFilter)
    // Merge duplicate actors into the appropriate user in the users list
    val merged = users.map(Person.fromUserInfo).map { p =>
      dupes
        .find(_.dataportenUser.exists { prefix =>
          p.dataportenUser.exists(du => du.toLowerCase.startsWith(prefix.toLowerCase))
        })
        .map { a =>
          p.copy(applicationId = a.applicationId)
        }
        .getOrElse(p)
    }
    // Return a union of the de-duped actors list and the users list
    nodup.union(merged)
  }

}
