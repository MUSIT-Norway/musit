package utils.testdata

import models.document.{ArchiveAddContext, ArchiveContext}
import no.uio.musit.models.{ActorId, MuseumCollections, MuseumId}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}

trait BaseDummyData {

  val defaultMuseumId = MuseumId(99)

  val defaultActorId = ActorId.generate()

  val dummyUser: AuthenticatedUser =
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

  val dummyContext: ArchiveContext = ArchiveContext(dummyUser, defaultMuseumId)

  val dummyAddContext: ArchiveAddContext =
    ArchiveAddContext(dummyUser, defaultMuseumId, MuseumCollections.Archeology.uuid)

}
