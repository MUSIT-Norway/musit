package services.actor

import models.actor.Person
import no.uio.musit.security.AuthenticatedUser

import scala.concurrent.Future

class UserService {

  /**
   * Gets an actor representing the current user. This is now a silly service!
   *
   * @param user the current AuthenticatedUser.
   * @return a Future Person representation of the current user.
   */
  def currentUserAsActor(user: AuthenticatedUser): Future[Person] = {
    Future.successful(Person.fromAuthUser(user))
  }
}
