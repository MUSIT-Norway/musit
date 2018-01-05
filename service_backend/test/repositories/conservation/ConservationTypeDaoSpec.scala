package repositories.conservation

import java.util.UUID

import no.uio.musit.models.MuseumCollections.Entomology
import no.uio.musit.models.{ActorId, GroupId, MuseumId}
import no.uio.musit.security._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll
import repositories.conservation.dao.ConservationTypeDao

class ConservationTypeDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: ConservationTypeDao = fromInstanceCache[ConservationTypeDao]

  val dummyUid = ActorId.generate()

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(
      uuid = SessionUUID.generate(),
      oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
      userId = Option(dummyUid),
      isLoggedIn = true
    ),
    userInfo = UserInfo(
      id = dummyUid,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq(
      GroupInfo(
        id = GroupId.generate(),
        name = "FooBarGroup",
        module = StorageFacility,
        permission = Permissions.Admin,
        museumId = MuseumId(99),
        description = None,
        collections = Seq()
      )
    )
  )

  "The ConserveringTypeDao" should {

    "return all conservation types for a specific collection" in {
      val entoUUID = Entomology.uuid // scalastyle:ignore

      val res = dao.allFor(Some(entoUUID)).value.futureValue

      val ats = res.successValue
      ats.size mustBe 9

      forAll(ats) { t =>
        (t.collections.contains(entoUUID) || t.collections.isEmpty) mustBe true
      }
    }
  }
}
