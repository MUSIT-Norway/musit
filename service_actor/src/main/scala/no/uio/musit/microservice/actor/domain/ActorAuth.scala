package no.uio.musit.microservice.actor.domain

import no.uio.musit.microservices.common.utils.ErrorHelper
import no.uio.musit.microservices.common.utils.Misc.MusitBool
import no.uio.musit.microservices.common.utils.Misc.boolToMusitBool
import no.uio.musit.security.AuthenticatedUser

/**
 * Created by jarle on 31.08.16.
 */
object ActorAuth {

  def canInsertActor(securityConnection: AuthenticatedUser, person: Person) = {
    true //Todo: !!!
  }

  def genericVerify(condition: () => Boolean, devText: String): MusitBool = {
    boolToMusitBool(condition(), ErrorHelper.forbidden("", devText))
  }

  def verifyCanInsertActor(securityConnection: AuthenticatedUser, person: Person): MusitBool = {
    genericVerify(() => canInsertActor(securityConnection, person), "Not allowed to insert actor")
  }
}
