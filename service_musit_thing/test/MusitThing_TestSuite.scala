/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.service_musit_thing.dao.MusitThingDao
import no.uio.musit.microservice.service_musit_thing.domain.MusitThing
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{FunSuite, Matchers}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class MusitThing_TestSuite extends FunSuite with Matchers with ScalaFutures {

  val additionalConfiguration:Map[String, String] = Map.apply (
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver" , "org.h2.Driver"),
    ("slick.dbs.default.db.url" , "jdbc:h2:mem:play-test"),
    ("evolutionplugin" , "enabled")
  )
  val timeout = PatienceConfiguration.Timeout(1 seconds)

  test("testInsertMusitThing") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      MusitThingDao.insert(MusitThing(1, "C2", "spyd", Seq.empty))
      MusitThingDao.insert(MusitThing(2, "C3", "øks", Seq.empty))
      val svar=MusitThingDao.all()
      svar.onFailure{
        case ex => fail("Insert failed")
      }
      whenReady(svar, timeout) { things =>
        assert (things.length == 4)
      }
    }
  }
  
  test("getDisplayName_kjempeTall") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayName(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }

  test("getDisplayName_Riktig") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayName(2)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some("Kniv7"))
      }
    }
  }

  test("getDisplayName_TalletNull") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayName(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }

  test("getDisplayID_kjempeTall") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayID(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }

  test("getDisplayID_Riktig") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayID(2)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some("C2"))
      }
    }
  }

  test("getDisplayID_TalletNull") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getDisplayID(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }

  test("getById_kjempeTall") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getById(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }

  test("getById__Riktig") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getById(1)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some(MusitThing(1,"C1","Øks5", Seq(LinkService.self("/v1/1")))))
      }
    }
  }

  test("getById__TalletNull") {
    running(FakeApplication(additionalConfiguration = additionalConfiguration)) {
      val svar = MusitThingDao.getById(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }
}
