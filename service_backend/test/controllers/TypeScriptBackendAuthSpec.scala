package controllers

import no.uio.musit.models.MuseumId
import no.uio.musit.security.Permissions._
import no.uio.musit.security._
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.test.Helpers._

//Hint, to run only this test, type:
//test-only controllers.TypeScriptBackendAuthSpec

class TypeScriptBackendAuthSpec extends MusitSpecWithServerPerSuite {

  val mid        = MuseumId(99)
  val tokenAdmin = BearerToken(FakeUsers.testAdminToken)
  val tokenRead  = BearerToken(FakeUsers.testReadToken)
  val tokenTest  = BearerToken(FakeUsers.testUserToken)

  val baseUrl = s"/authorize"
  def url(
      optMid: Option[Int],
      optModule: Option[Int],
      roles: Seq[Int]
  ): String = {
    var params = ""
    def addParam(p: String) = {
      params = if (params.isEmpty) p else s"$params&$p"
    }

    optMid.map(mid => addParam(s"museumId=$mid"))

    optModule.map(module => addParam(s"moduleConstraintId=$module"))

    roles.foreach(r => addParam(s"permissions=$r"))

    s"$baseUrl${if (params.isEmpty) "" else s"?$params"}"

  }

  def getResult(
      musId: Option[Int] = Some(mid.underlying),
      module: Option[Int] = None,
      permissions: Seq[Int],
      token: BearerToken
  ) = {
    wsUrl(url(musId, module, permissions))
      .withHttpHeaders(token.asHeader)
      .get()
      .futureValue
  }

  def getResultWithoutToken(
      musId: Option[Int] = Some(mid.underlying),
      module: Option[Int] = Some(0),
      permissions: Seq[Int] = Seq()
  ) = {
    wsUrl(url(musId, module, permissions)).get().futureValue
  }

  "TypeScriptBackendEndAuth" when {

    "used without permissions on a controller" should {

      "return HTTP Unauthorized (401, really means unauthenticated) if bearer token is missing" in {

        val res = getResultWithoutToken(permissions = Seq(Read.priority))

        res.status mustBe UNAUTHORIZED
      }
      "return Http Forbidden if someone with only Read tries to Write" in {

        val res = getResult(permissions = Seq(Write.priority), token = tokenRead)
        res.status mustBe FORBIDDEN

      }

    }
    "used with permissions on a controller" should {
      "return HTTP Ok if someone with read access tries to read" in {

        val res =
          getResult(module = None, permissions = Seq(Read.priority), token = tokenRead)

        println(s"${res.statusText}")
        res.status mustBe OK
      }
    }
    "used with invalid parameters" should {
      "return validation error if unexisting museum id" in {

        val res =
          getResult(
            musId = Some(123456),
            permissions = Seq(Read.priority),
            token = tokenRead
          )

        res.status mustBe BAD_REQUEST
      }

      "return validation error if unexisting module id" in {

        val res =
          getResult(
            module = Some(123456),
            permissions = Seq(Read.priority),
            token = tokenRead
          )

        res.status mustBe BAD_REQUEST
      }
      "return validation error if unexisting permission id" in {

        val res =
          getResult(permissions = Seq(123456), token = tokenRead)

        res.status mustBe BAD_REQUEST
      }

    }

  }

}
