package no.uio.musit.security.dataporten

import java.util.UUID

import no.uio.musit.models.{ActorId, Email}
import no.uio.musit.security.{Authenticator, BearerToken, SessionUUID}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Inside, OptionValues}
import play.api.Configuration
import play.api.http.{DefaultWriteables, Writeable}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._

class DataportenAuthenticatorSpec
    extends MusitSpecWithAppPerSuite
    with MockFactory
    with DefaultWriteables
    with OptionValues
    with Inside {

  implicit val conf = fromInstanceCache[Configuration]
  val resolver      = fromInstanceCache[DatabaseAuthResolver]

  val mockWS         = mock[WSClient]
  val mockWSRequest  = mock[WSRequest]
  val mockWSResponse = mock[WSResponse]

  val authenticator = new DataportenAuthenticator(resolver, mockWS)

  val userId    = ActorId.generate()
  val userIdSec = Email("vader@deathstar.io")
  val name      = "Darth Vader"
  val email     = Email("darth.vader@deathstar.io")

  type FormDataType = Map[String, Seq[String]]

  "DataportenAuthenticator" should {

    var sessionId = ""

    "initialise a new UserSession when starting the authentication process" in {

      implicit val fakeRequest = FakeRequest("GET", "/authenticate")

      val futRes = authenticator.authenticate(Some(Authenticator.ClientWeb))
      val res    = futRes.futureValue

      inside(res) {
        case Left(left) =>
          left mustBe a[Result]
          val redirectLoc = redirectLocation(Future.successful(left))
          sessionId = redirectLoc.value.substring(redirectLoc.value.lastIndexOf('=') + 1)
          SessionUUID.validate(sessionId).isSuccess mustBe true
      }
    }

    "fetch an access token and update the UserSession when receiving a code" in {
      val code      = UUID.randomUUID().toString
      val token     = BearerToken(UUID.randomUUID().toString)
      val expiresIn = (2 hours).toMillis
      implicit val fakeRequest = FakeRequest(
        method = "POST",
        path = s"/authenticate?code=$code&state=$sessionId"
      )
      val expQueryParams = Map(
        "code"  -> Seq(code),
        "state" -> Seq(sessionId)
      )

      // Setup some mocks to avoid actually calling Dataporten.
      (mockWS.url _)
        .expects("https://auth.dataporten.no/oauth/token")
        .returning(mockWSRequest)

      (mockWSRequest
        .post(_: FormDataType)(_: Writeable[FormDataType]))
        .expects(*, *)
        .returning(Future.successful(mockWSResponse))

      (mockWSResponse.json _)
        .expects()
        .returning(
          Json.obj(
            "access_token" -> token.underlying,
            "expiresIn"    -> expiresIn
          )
        )

      (mockWS.url _)
        .expects("https://auth.dataporten.no/userinfo")
        .returning(mockWSRequest)

      (mockWSRequest.withHeaders _).expects(*).returning(mockWSRequest)
      // The below mock of "get" is incorrectly highlighted with red in IntelliJ.
      // This is a known bug: https://youtrack.jetbrains.com/issue/SCL-10183
      (mockWSRequest.get _).expects().returning(Future.successful(mockWSResponse))
      (mockWSResponse.status _).expects().returning(OK)
      (mockWSResponse.json _)
        .expects()
        .returning(
          Json.obj(
            "audience" -> "b98123fe-3a25-44b9-8e26-75819d8aa5da",
            "user" -> Json.obj(
              "userid"     -> userId.asString,
              "userid_sec" -> Json.arr(userIdSec.value),
              "name"       -> name,
              "email"      -> email.value
            )
          )
        )
        .atLeastTwice()

      val futRes = authenticator.authenticate(Some(Authenticator.ClientWeb))

      val res = futRes.futureValue
      res.isRight mustBe true

      inside(res) {
        case Right(session) =>
          session.uuid mustBe SessionUUID.unsafeFromString(sessionId)
          session.isLoggedIn mustBe true
          session.lastActive must not be None
          session.oauthToken mustBe Some(token)
          session.userId mustBe Some(userId)
          session.loginTime must not be None
      }
    }

  }

}
