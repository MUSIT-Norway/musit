package no.uio.musit.microservice.actor.service

import com.google.inject.Inject
import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.security.AuthenticatedUser
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class UserService @Inject() (val actorDao: ActorDao) {

  /**
   * Gets an actor representing the current user. If it doesn't exist one in the
   * database, it creates one.
   *
   * @param user
   * @return
   */
  def currenUserAsActor(user: AuthenticatedUser): Future[Person] = {
    actorDao.getPersonByDataportenId(user.userInfo.id).flatMap {
      case Some(person) => Future.successful(person)
      case None => actorDao.insertAuthenticatedUser(user)
    }
  }
}
