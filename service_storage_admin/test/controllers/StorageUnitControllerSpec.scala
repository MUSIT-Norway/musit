package controllers

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain.{ StorageBuilding, StorageRoom, StorageUnit }
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsString, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class StorageUnitControllerSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "addStorageUnit" should {
    import StorageUnitDao._
    "testInsertStorageUnit" in {
      val svar = for {
        building <- insertBuilding(StorageUnit(None, "Building", "KHM_ØstreAker", Some(20), Some("1"), None, Some(1),
          Some("skriv"), Some("les"), None), StorageBuilding(None, Some("Østre Akervei 3"), None))

        room <- insertRoom(StorageUnit(None, "Room", "ROM1", Some(10), Some("1"), None, Some(20),
          Some("skriv"), Some("les"), None), StorageRoom(None, Some("1"), Some("1"),
          Some("1"), Some("1"), Some("1"), Some("1"), Some("1"), Some("1"),
          None))

        storageUnit <- insertAndRun(StorageUnit(None, "StorageUnit", "HYLLE1", Some(5), Some("1"), building._1.id, Some(5),
          Some("skriv"), Some("les"), None))

        svarTemp <- StorageUnitDao.all()

      } yield svarTemp

      //val svar = StorageUnitDao.all()
      svar.onFailure {
        case ex => fail(s"Insert failed:${ex.getMessage} ")
      }
      svar.onSuccess {
        case stUnitSeq => assert(stUnitSeq.length == 3)

      }
    }

    "getSubNodes" in {
      val svar = StorageUnitDao.getChildren(1)
      whenReady(svar, timeout) { stUnit =>
        assert(stUnit.length == 1)
      }
    }

    "getById__Riktig" in {
      val svar = getById(3)
      whenReady(svar, timeout) { storageUnit =>
        assert(storageUnit.contains(StorageUnit(Some(3), "StorageUnit", "HYLLE1", Some(5), Some("1"), Some(1), Some(5),
          Some("skriv"), Some("les"), Some(Seq(LinkService.self("/v1/3"))))))
      }
    }

  }
}