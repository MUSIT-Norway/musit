/**
  * Created by ellenjo on 4/15/16.
  */


import no.uio.musit.microservice.geoLocation.dao.GeoLocationDao
import no.uio.musit.microservice.geoLocation.domain.GeoLocation
import no.uio.musit.microservice.geoLocation.service.GeoLocationService
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class GeoLocation_TestSuite extends PlaySpec with OneAppPerSuite with ScalaFutures with GeoLocationService{

  val additionalConfiguration:Map[String, String] = Map.apply (
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver" , "org.h2.Driver"),
    ("slick.dbs.default.db.url" , "jdbc:h2:mem:play-test"),
    ("evolutionplugin" , "enabled")
  )
  val timeout = PatienceConfiguration.Timeout(1 seconds)
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  "GeoLocationDao slick dao" must {
    import GeoLocationDao._

    "testInsertGeoLocation" in {
      insert(GeoLocation(10, "St. Olavsgate 3", Seq.empty))
      insert(GeoLocation(11, "urkeGata 666", Seq.empty))
      val svar=GeoLocationDao.all()
      svar.onFailure{
        case ex => fail("Insert failed")
      }
      whenReady(svar, timeout) { geoLocations =>
        assert (geoLocations.length == 4)
      }
    }

    "getById_kjempeTall" in {
      val svar = getById(6386363673636335366L)
      whenReady(svar, timeout) { geoLocation =>
        assert (geoLocation == None)
      }
    }

    "getById__Riktig" in {
      val svar = getById(1)
      whenReady(svar, timeout) { geoLocation =>
        assert (geoLocation == Some(GeoLocation(1,"Frederiksgate 3", Seq(LinkService.self("/v1/1")))))
      }
    }

    "getById__TalletNull" in {
      val svar = getById(0)
      whenReady(svar, timeout) { geoLocation =>
        assert (geoLocation == None)
      }
    }

    "searchAddress" in {
      val svar = this.searchGeoNorway("paal bergs vei 56, RYKKINN")
      whenReady(svar, timeout) { geoAddresses =>
      assert (geoAddresses.length > 0)
      }
      }

  }

}
