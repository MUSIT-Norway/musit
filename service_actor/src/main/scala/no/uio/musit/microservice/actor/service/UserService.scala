package no.uio.musit.microservice.actor.service

/**
 * Created by jarle on 13.09.16.
 */

import com.google.inject.Inject
import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.domain.{ MusitError, MusitSearch, MusitStatusMessage }
import no.uio.musit.microservices.common.extensions.FutureExtensions.MusitFuture
import no.uio.musit.security.SecurityConnection
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class UserService @Inject() (val actorDao: ActorDao) {
  //Gets an actor representing the current user. If it doesn't exist one in the database, it creates one.
  def getCurrentUserAsActor(securityConnection: SecurityConnection): MusitFuture[Person] = {
    val futureActor = actorDao.getPersonByDataportenId(securityConnection.userId)
    futureActor.flatMap {
      case Some(person) => MusitFuture.successful(person)
      case None => actorDao.insertActorWithDataportenUserInfo(securityConnection)
    }
  }

}
