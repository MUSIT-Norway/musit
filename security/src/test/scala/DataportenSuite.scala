/**
  * Created by jstabel on 3/31/16.
  */

import java.lang.Exception

import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.extensions.PlayExtensions.{MusitAuthFailed, MusitBadRequest}
import org.scalatest.FunSuite
import no.uio.musit.security._
import no.uio.musit.security.dataporten.Dataporten
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util
import scala.util.{Failure, Success}

class DataportenSuite extends PlayDatabaseTest with ScalaFutures {
  val expiredToken = "59197195-bf27-4ab1-bf57-b460ed85edab"
  //TEMP!!
  val token = "6fa97170-7b71-4b9c-aa71-158ab33e0b45"

  override protected def beforeAll(): Unit = {
    super.beforeAll()


    println("hallo")
    /*
        val f = Dataporten.createSecurityConnection(token+"a")
        f.onComplete {
          case Success(s) => println("!!!Success")
          case Failure(ex) => println(s"!!!Å nei! ${ex.getMessage}")
        }*/
  }

  override protected def afterAll(): Unit = {
    // Wait for futures
    printf("Tearing down")
    super.afterAll()
  }

  //val fut=Dataporten.createSecurityConnection(token)
  //fut.futureValue.


  val fut = Dataporten.createSecurityConnection(token)
  fut.map { sec =>
    //val context = new Context(token)


    test("getUserInfo should return something") {
      val userName = sec.userName
      assert(userName == "Jarle Stabell")
      assert(userName.length > 0)
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

      intercept[Exception] {

        sec.authorize(Seq(Groups.DS, "blablabla")) {
          Future(assert(true == false))
        }
      }
    }

    test("Structurally invalid Context/token should fail give bad request") {
      val f = Dataporten.createSecurityConnection("tullballtoken")
      f.onComplete {
        case Success(s) => fail("Skulle ikke få connection med tullballtoken")
        case Failure(ex) => () //assert(true==false)
      }
      ScalaFutures.whenReady(f.failed) { e => e shouldBe a[MusitBadRequest] }
    }
    test("Invalid Context/token should fail give auth error") {
      val f = Dataporten.createSecurityConnection("59197195-bf27-4ab1-bf57-b460ed85abba")
      f.onComplete {
        case Success(s) => fail("Skulle ikke få connection med ugyldig token")
        case Failure(ex) => println("bra!") //assert(true==false)
      }
      ScalaFutures.whenReady(f.failed) { e => e shouldBe a[MusitAuthFailed] }
    }
    /*
          try {
            val answer = Await.result(f, 20 seconds)
          } catch {
            case e: IllegalStateException => println("fanget exception...")
          }
    */



    println("halloFerdig")

    //        ).onFailure{case _ => assert(true==false) /*fail("Skulle ikke få connection med tullballtoken")*/ }

  }.onComplete {
    case Success(ok) => println("ok")
    case Failure(ex) => println(s"Unable to connect: ${ex.getMessage}")
  }

  val answer = Await.result(fut, 20 seconds)

}