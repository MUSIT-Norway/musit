/**
  * Created by jstabel on 3/31/16.
  */

import org.scalatest.FunSuite
import no.uio.musit.security._
import no.uio.musit.security.dataporten.Dataporten

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DataportenSuite extends FunSuite {

  val token = "d9cb3750-b322-495c-aaab-cef9bf34a235" //TEMP!!
  val sec = Dataporten.createSecuritySupport(token)
  //val context = new Context(token)



  test("getUserInfo should return something") {
    val userName = sec.userName
    assert(userName=="Jarle Stabell")
    assert(userName.length>0)
  }

  test("Authorize for DS") {
    sec.authorize(Seq(Groups.DS)) {
      Future(println("Har DS gruppa!"))
    }
  }

  test("Authorize for DS og konservator les") {
    sec.authorize(Seq(Groups.DS, Groups.MusitKonservatorLes)) {
      Future(println("Har DS og konservatorLes gruppa!"))
    }
  }

  test("Authorize for ugyldig gruppe") {
    sec.authorize(Seq(Groups.DS, "blablabla")) {
      Future(assert(true==false))
    }
  }

  test("Invalid Context/token should fail") {
    intercept[Exception] {
      val invalidContext = Dataporten.createSecuritySupport("tullballtoken")
      val userName = sec.userName
      assert(userName=="Jarle Stabell")
    }
  }


}