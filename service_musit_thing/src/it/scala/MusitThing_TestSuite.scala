/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.musitThing.dao.MusitThingDao
import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MusitThing_TestSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  override lazy val port: Int = 19003

  "MusitThing integration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/1").get()
      whenReady(future, timeout) { response =>
        val json = Json.parse(response.body)
        assert((json \ "id").getOrElse(JsString("0")).toString() == "1")
      }
    }
  }

  "MusitThing slick dao" must {
    import MusitThingDao._

    "testInsertMusitThing" in {
      insert(MusitThing(Some(1), "C2", "spyd", None))
      insert(MusitThing(Some(2), "C3", "øks", None))
      val svar=MusitThingDao.all()
      svar.onFailure{
        case ex => fail("Insert failed")
      }
      whenReady(svar, timeout) { things =>
        assert (things.length == 4)
      }
    }

    "getDisplayName_kjempeTall" in {
      val svar = getDisplayName(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getDisplayName_Riktig" in {
      val svar = getDisplayName(2)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some("Kniv7"))
      }
    }

    "getDisplayName_TalletNull" in {
      val svar = getDisplayName(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getDisplayID_kjempeTall" in {
      val svar = getDisplayID(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getDisplayID_Riktig" in {
      val svar = getDisplayID(2)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some("C2"))
      }
    }

    "getDisplayID_TalletNull" in {
      val svar = getDisplayID(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getById_kjempeTall" in {
      val svar = getById(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getById__Riktig" in {
      val svar = getById(1)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some(MusitThing(Some(1),"C1","Øks5", Some(Seq(LinkService.self("/v1/1"))))))
      }
    }

    "getById__TalletNull" in {
      val svar = getById(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }
}
