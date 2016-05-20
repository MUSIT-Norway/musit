/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Actor
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

class ActorUnitTest extends PlaySpec with OneAppPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "Actor dao" must {
    import ActorDao._


    "getById_kjempeTall" in {
      val svar = getActorById(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getById__Riktig" in {
      val svar = getActorById(1)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some(Actor(1,"And, Arne1", Seq(LinkService.self("/v1/1")))))
      }
    }

    "getById__TalletNull" in {
      val svar = getActorById(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }
}
