package controllers

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.{StorageBuilding, StorageRoom, StorageUnit}
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class StorageUnitControllerSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "addStorageUnit" should {
    import StorageUnitDao._
    "testInsertStorageUnit" in {
      val svar = for {
          building <- insertBuilding(StorageUnit(-1, "Building", "KHM_ØstreAker", Some(20), Some("1"), None, Some(1),
        Some("skriv"), Some("les"), Seq.empty), StorageBuilding(-1, Some("Østre Akervei 3"), Seq.empty))
      room <- insertRoom(StorageUnit(-1, "Room", "ROM1", Some(10), Some("1"), None, Some(20),
        Some("skriv"), Some("les"), Seq.empty), StorageRoom(-1, Some("1"), Some("1"),
        Some("1"), Some("1"), Some("1"), Some("1"), Some("1"), Some("1"),
        Seq.empty))
      storageUnit <- insert(StorageUnit(-1, "StorageUnit", "HYLLE1", Some(5), Some("1"), Some(building._1.id), Some(5),
        Some("skriv"), Some("les"), Seq.empty))

        svarTemp <- StorageUnitDao.all()

      } yield svarTemp


      //val svar = StorageUnitDao.all()
      svar.onFailure {
        case ex => fail(s"Insert failed:${ex.getMessage} ")
      }
      svar.onSuccess {
        case stunit => assert(stunit.length == 3)
        case other => fail("aa")
      }
    }

    /*: Long, storageType: String, storageUnitName: String, area: Option[Long],
    isStorageUnit: Option[String], isPartOf: Option[Long], height: Option[Long],
    groupRead: Option[String], groupWrite: Option[String],*/


    "getSubNodes" in {
      val svar = StorageUnitDao.getChildren(1)
      whenReady(svar, timeout) { stUnit =>
        assert(stUnit.length == 1)
      }
    }

    "getById__Riktig" in {
      val svar = getById(3)
      whenReady(svar, timeout) { storageUnit =>
        assert (storageUnit == Some(StorageUnit(3, "StorageUnit", "HYLLE1", Some(5), Some("1"), Some(1), Some(5),
          Some("skriv"), Some("les"),  Seq(LinkService.self("/v1/3")))))
      }
    }





    "futuretest" in {
      val testF: Future[Int] = Future {
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