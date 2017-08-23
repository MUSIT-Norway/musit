package no.uio.musit.service

import no.uio.musit.models.ActorId
import no.uio.musit.security.Permissions._
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.fake.FakeAuthenticator
import no.uio.musit.security.{BearerToken, EncryptedToken}
import no.uio.musit.test.{FakeUsers, MusitSpecWithAppPerSuite}
import play.api.mvc.BaseController
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class MusitAdminActionSpec extends MusitSpecWithAppPerSuite {

  implicit lazy val materializer = app.materializer
  implicit lazy val ec           = app.actorSystem.dispatcher

  implicit val musitCrypto = fromInstanceCache[MusitCrypto]

  class Dummy extends BaseController with MusitAdminActions {
    val controllerComponents = stubControllerComponents()
    override val authService = new FakeAuthenticator
    override val crypto      = musitCrypto
  }

  val request = (uri: String) => FakeRequest(GET, uri)

  def authAction(userId: ActorId, token: BearerToken) = {
    new Dummy().MusitAdminAction() { request =>
      request.token mustBe token
      request.user.userInfo.id mustBe userId
      Ok(request.user.userInfo.id.asString)
    }
  }

  def authActionWithPerms(
      userId: ActorId,
      token: BearerToken,
      perms: ElevatedPermission*
  ) = {
    new Dummy().MusitAdminAction(perms: _*) { request =>
      request.token mustBe token
      request.user.userInfo.id mustBe userId
      Ok(request.user.userInfo.id.asString)
    }
  }

  val unauthAction = new Dummy().MusitAdminAction()(request => Ok)

  val superUserId    = ActorId.unsafeFromString(FakeUsers.superUserId)
  val superUserToken = BearerToken(FakeUsers.superUserToken)

  " A MusitAdminAction" when {

    "initialized with an invalid permission" should {
      "not compile" in {
        "new Dummy().MusitAdminAction(Read)(r => Ok)" mustNot compile
      }

      "not pass type checking if permission is of incorrect type" in {
        """new Dummy().MusitAdminAction("Read")(r => Ok)""" mustNot typeCheck
      }
    }

    "used without permissions on a controller" should {

      "return HTTP Unauthorized if bearer token is missing" in {
        val res = call(unauthAction, request("/"))
        status(res) mustEqual UNAUTHORIZED
      }

      "return OK if the request has a valid token as query parameter" in {
        val encToken = EncryptedToken.fromBearerToken(superUserToken)

        val action = authAction(superUserId, superUserToken)
        val req    = request(s"/param?_at=${encToken.urlEncoded}")
        val res    = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(superUserId.asString)
      }

      "return OK if the request has a valid bearer token in the Auth header" in {
        val action = authAction(superUserId, superUserToken)
        val req    = request("/").withHeaders(superUserToken.asHeader)
        val res    = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(superUserId.asString)
      }

      "return HTTP Forbidden user has insufficient access rights" in {
        val uid = ActorId.unsafeFromString(FakeUsers.normalUserId)
        val tok = BearerToken(FakeUsers.normalUserToken)

        val res = call(authAction(uid, tok), request("/").withHeaders(tok.asHeader))

        status(res) mustEqual FORBIDDEN
      }

    }

    "used with permissions on a controller" should {

      "return Forbidden if user has insufficient access rights" in {
        val uid = ActorId.unsafeFromString(FakeUsers.normalUserId)
        val tok = BearerToken(FakeUsers.normalUserToken)

        val req = request("/").withHeaders(tok.asHeader)
        val res = call(authActionWithPerms(uid, tok), req)

        status(res) mustEqual FORBIDDEN
      }

      "return OK if the user is god" in {
        val req = request("/").withHeaders(superUserToken.asHeader)
        val res = call(authActionWithPerms(superUserId, superUserToken), req)

        status(res) mustEqual OK
        contentAsString(res) must include(superUserId.asString)
      }

      "return OK if the user is a DB coordinator" in {
        val uid = ActorId.unsafeFromString(FakeUsers.dbCoordId)
        val tok = BearerToken(FakeUsers.dbCoordToken)

        val req = request("/").withHeaders(tok.asHeader)
        val res = call(authActionWithPerms(uid, tok), req)

        status(res) mustEqual OK
        contentAsString(res) must include(uid.asString)
      }

    }

  }

}
