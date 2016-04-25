/**
  * Created by jstabel on 3/31/16.
  */

import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.extensions.PlayExtensions.{MusitAuthFailed, MusitBadRequest}
import no.uio.musit.security._
import no.uio.musit.security.dataporten.Dataporten
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

import scala.concurrent.Future
import scala.concurrent.duration._

class DataportenSuite extends PlayDatabaseTest with ScalaFutures {
  val expiredToken = "59197195-bf27-4ab1-bf57-b460ed85edab"
  // TODO: Dynamic token, find a way to have a permanent test token with Dataporten
  val token = "259239f9-4012-4c62-8de5-a1753931445e"
  var fut: Future[SecurityConnection] = null

  def timeout = PatienceConfiguration.Timeout(1 seconds)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    //This can't be in the constructor, it has to be after the setup because createSecurityConnection accesses the WS object.
    fut = Dataporten.createSecurityConnection(token)
  }

  test("getUserInfo should return something") {
    whenReady(fut, timeout) { sec =>
      val userName = sec.userName
      assert(userName == "Jarle Stabell")
      assert(userName.length > 0)
    }
  }

  test("Authorize for DS") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS)) {}.isSuccess)
    }
  }

  test("Authorize for DS and MusitKonservatorLes") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS, Groups.MusitKonservatorLes)) {}.isSuccess)
    }
  }

  test("Authorize for invalid group") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS, "invalid groupid")) {}.isFailure)
    }
  }

  test("Structurally invalid Context/token should fail give bad request") {
    val f = Dataporten.createSecurityConnection("tullballtoken")
    whenReady(f.failed, timeout) { e => e shouldBe a[MusitBadRequest] }
  }

  test("Invalid Context/token should fail give auth error") {
    val f = Dataporten.createSecurityConnection("59197195-bf27-4ab1-bf57-b460ed85abba")
    whenReady(f.failed, timeout) { e => e shouldBe a[MusitAuthFailed] }
  }
}