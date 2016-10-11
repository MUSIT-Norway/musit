package no.uio.musit.microservice.actor.domain

import no.uio.musit.microservices.common.utils.ErrorHelper
import no.uio.musit.microservices.common.utils.Misc.MusitBool
import no.uio.musit.microservices.common.utils.Misc.boolToMusitBool
import no.uio.musit.security.AuthenticatedUser

object ActorAuth {

  def canInsertActor(user: AuthenticatedUser, person: Person) = {
    true //Todo: !!!
  }

  def genericVerify(condition: () => Boolean, devText: String): MusitBool = {
    boolToMusitBool(condition(), ErrorHelper.forbidden("", devText))
  }

  def verifyCanInsertActor(user: AuthenticatedUser, person: Person): MusitBool = {
    genericVerify(() => canInsertActor(user, person), "Not allowed to insert actor")
  }
}
