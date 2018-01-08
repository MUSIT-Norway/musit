package services.geolocation

import no.uio.musit.MusitResults.MusitHttpError
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inside
import play.api.Configuration
import play.api.http.Status
import play.api.mvc._
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server

import scala.concurrent.ExecutionContext.Implicits.global

class GeoLocationServiceSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside {

  val service: GeoLocationService = fromInstanceCache[GeoLocationService]

  implicit val sys = app.actorSystem
  implicit val mat = app.materializer

  "GeoLocationService" when {

    "searching for addresses" should {

      "return a list of results that match the query" in {
        val res = service.searchGeoNorway("paal bergs vei 56, RYKKINN").futureValue
        res.isSuccess mustBe true
        res.successValue.toList match {
          case theHead :: tail =>
            theHead.street mustBe "Paal Bergs vei"
            theHead.streetNo mustBe "56"
            theHead.place mustBe "RYKKINN"
            theHead.zip mustBe "1348"

          case _ =>
            fail(s"The list contained something wrong.")
        }

      }

      "return a list of results that contains street number with house letter" in {
        val res = service.searchGeoNorway("Kirkegata 11, Hønefoss").futureValue

        res.isSuccess mustBe true
        res.successValue.toList.find { a =>
          a.street == "Kirkegata" &&
          a.streetNo == "11 E" &&
          a.place == "HØNEFOSS" &&
          a.zip == "3510"
        } must not be None

      }

      "return 503 if service not available" in {
        implicit val conf = Configuration.from(
          Map(
            "musit.geoLocation.geoNorway.url"           -> "/geo",
            "musit.geoLocation.geoNorway.hitsPerResult" -> "4"
          )
        )
        val expMsg = "error message from GeoNorway"

        val ab = new DefaultActionBuilderImpl(new BodyParsers.Default)

        Server.withRouter() {
          case GET(p"/geo") =>
            ab.apply {
              Results.ServiceUnavailable(expMsg)
            }
        } { implicit port =>
          WsTestClient.withClient { implicit client =>
            val address = new GeoLocationService().searchGeoNorway("").futureValue
            address.isFailure mustBe true
            inside(address) {
              case MusitHttpError(status, msg) =>
                status mustBe Status.SERVICE_UNAVAILABLE
                msg mustBe expMsg
            }
          }
        }
      }
    }
  }
}
