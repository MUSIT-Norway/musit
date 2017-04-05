package services.analysis

import no.uio.musit.models.ActorId
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.{DateTimeMatchers, MusitResultValues}
import org.scalatest.OptionValues

class AnalysisServiceSpec
    extends MusitSpecWithAppPerSuite
    with DateTimeMatchers
    with MusitResultValues
    with OptionValues {

  val defaultUserId = ActorId.generate()
  val dummyActorId  = ActorId.generate()

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(uuid = SessionUUID.generate()),
    userInfo = UserInfo(
      id = defaultUserId,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq.empty
  )

  val service = fromInstanceCache[AnalysisService]

  "" should {

    "" in {
      pending
    }

  }

}
