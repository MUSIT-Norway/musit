/**
  * Created by jstabel on 3/31/16.
  */

import org.scalatest.FunSuite
import no.uio.musit.security._
import scala.concurrent.duration._

import scala.concurrent.Await

class DataportenSuite extends FunSuite {

  val token = "4f58cce4-e995-40e4-91bd-6af171a01ef1" //TEMP!!

  val context = new Context(token)
  test("getUserInfo should return something") {
    val futAnswer = dataporten.RawServices.getUserInfo(context)
    val answer = Await.result(futAnswer, 2 seconds)
    assert(answer.name=="Jarle Stabell")
    assert(answer.name.length>0)
  }


  test("getUserGroups should return something") {
    val futAnswer = dataporten.RawServices.getUserGroups(context)
    val answer = Await.result(futAnswer, 2 seconds)

    assert(answer.length>0)
  }

   val invalidContext = new Context("tullballtoken")
  test("Invalid Context/token should fail") {
    val futAnswer = dataporten.RawServices.getUserInfo(invalidContext)
    intercept[Exception] {
      val answer = Await.result(futAnswer, 2 seconds)
    }
  }


}