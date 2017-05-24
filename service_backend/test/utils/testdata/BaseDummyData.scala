package utils.testdata

import no.uio.musit.models.{ActorId, MuseumId}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}

trait BaseDummyData {

  val defaultMuseumId = MuseumId(99)

  val defaultActorId = ActorId.generate()

  implicit val dummyUser =
    AuthenticatedUser(
      session = UserSession(uuid = SessionUUID.generate()),
      userInfo = UserInfo(
        id = defaultActorId,
        secondaryIds = Some(Seq("vader@starwars.com")),
        name = Some("Darth Vader"),
        email = None,
        picture = None
      ),
      groups = Seq.empty
    )

}
