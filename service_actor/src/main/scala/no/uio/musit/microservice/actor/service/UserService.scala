package no.uio.musit.microservice.actor.service

import com.google.inject.Inject
import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.extensions.FutureExtensions.MusitFuture
import no.uio.musit.security.AuthenticatedUser
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class UserService @Inject() (val actorDao: ActorDao) {
  //Gets an actor representing the current user. If it doesn't exist one in the database, it creates one.
  def getCurrentUserAsActor(user: AuthenticatedUser): MusitFuture[Person] = {
    val futureActor = actorDao.getPersonByDataportenId(user.userId)
    futureActor.flatMap {
      case Some(person) => MusitFuture.successful(person)
      case None => actorDao.insertActorWithDataportenUserInfo(user)
    }
  }
}
