package controllers

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.StorageUnit
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class StorageUnitControllerSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "addStorageUnit" should {
    import StorageUnitDao._
    "testInsertStorageUnit" in {
      insert(StorageUnit(1, "ROM1", 20, "1", 0, 10, "Room", "Skriv", "Les", Seq.empty))
      insert(StorageUnit(2, "ROM2", 10, "1", 0, 20, "Room", "Skriv", "Les", Seq.empty))
      insert(StorageUnit(3, "HYLLE1", 5, "1", 1, 5, "StorageUnit", "Skriv", "Les", Seq.empty))
      val svar = StorageUnitDao.all()
      svar.onFailure {
        case ex => fail("Insert failed")
      }
      svar.onSuccess {
        case stunit => assert(stunit.length == 3)
        case other => fail("aa")
      }
    }

    "getSubNodes" in {
      val svar = StorageUnitDao.getSubNodes(1)
      whenReady(svar, timeout) { stUnit =>
       assert(stUnit.length == 1)
      }
    }


    "futuretest" in {
      val testF:Future[Int] = Future  {
        //throw new Exception("Å nei!")
        5
      }
      val testF2 = testF.map(_ + 10)
      println("hallo")
      val response = Await.ready(testF2, Duration.Inf)
      println(s"verdi: ${response.value}")
/*
      whenReady(testF2, timeout) { res =>
        println(s"verdi: ${testF2.value}")
      }
*/


    }
  }
}