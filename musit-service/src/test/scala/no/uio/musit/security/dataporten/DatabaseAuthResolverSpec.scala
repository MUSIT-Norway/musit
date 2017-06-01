package no.uio.musit.security.dataporten

import java.util.UUID

import no.uio.musit.models.{ActorId, Email}
import no.uio.musit.security.{BearerToken, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseAuthResolverSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  // Wire up the dependencies
  val config   = fromInstanceCache[DatabaseConfigProvider]
  val resolver = new DatabaseAuthResolver(config)

  val uuid  = ActorId.generate()
  val email = "dv@deathstar.io"

  val sessionUUID = SessionUUID.generate()

  val userInfo = UserInfo(
    id = uuid,
    secondaryIds = Option(Seq(email)),
    name = Option("Darth Vader"),
    email = Option(Email("darth.vader@deathstar.io")),
    picture = None
  )

  "The DatabaseAuthResolver" when {

    "working with UserInfo data" should {

      "insert new UserInfo" in {
        resolver.saveUserInfo(userInfo).futureValue.isSuccess mustBe true
      }

      "find a UserInfo by " in {
        val res = resolver.userInfo(uuid).futureValue
        res.successValue mustBe Some(userInfo)
      }

      "convert email fields to lower case when storing UserInfo" in {
        val ui = UserInfo(
          id = ActorId.generate(),
          secondaryIds = Option(Seq(email.toUpperCase)),
          name = Option("Darth Vader"),
          email = Option(Email("darth.vader@deathstar.io")),
          picture = None
        )

        resolver.saveUserInfo(ui).futureValue.isSuccess mustBe true

        val res = resolver.userInfo(ui.id).futureValue
        res.successValue.isDefined mustBe true
        res.successValue.value.secondaryIds.value.head mustBe email.toLowerCase
      }

      "update existing UserInfo" in {
        val upd = userInfo.copy(name = Some("Darth Anakin Vader"))
        resolver.saveUserInfo(upd).futureValue.successValue
        val res = resolver.userInfo(uuid).futureValue
        res.successValue mustBe Some(upd)
      }
    }

    "when working with UserSession data" should {

      "initialize an empty UserSession and return the SessionUUID" in {
        val res = resolver.upsertUserSession(UserSession(uuid = sessionUUID)).futureValue
        res.isSuccess mustBe true
      }

      "find and update an UserSession" in {
        val sessionRes = resolver.userSession(sessionUUID).futureValue
        val session    = sessionRes.successValue.value
        session.uuid mustBe sessionUUID

        val currDateTime = DateTime.now

        val upd = session.copy(
          oauthToken = Option(BearerToken(UUID.randomUUID().toString)),
          userId = ActorId.generateAsOpt(),
          loginTime = Some(currDateTime),
          lastActive = Some(currDateTime),
          isLoggedIn = true,
          tokenExpiry = Some(DateTime.now.plusHours(4).getMillis)
        )

        resolver.updateSession(upd).futureValue.successValue

        val res = resolver.userSession(sessionUUID).futureValue
        res.successValue mustBe Some(upd)
      }

    }

  }

}
