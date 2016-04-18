/**
  * Created by ellenjo on 4/15/16.
  */
import org.scalatest.FunSuite

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class MusitThing_TestSuite extends FunSuite{

  test("test2") {
    val verdi=5
    println("Ferdig2")

    assert(verdi==5)
  }

    test("test 1") {
      val verdi=5
      println("Ferdig1")

      assert(verdi==7)
    }
  test("test 3") {
    val verdi=5
    println("Ferdig3")

    assert(verdi==7)
  }
  }
