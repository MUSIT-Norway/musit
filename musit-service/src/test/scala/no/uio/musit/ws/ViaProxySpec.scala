package no.uio.musit.ws

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.ws.ProxiedRequest._
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSClient}

class ViaProxySpec extends MusitSpecWithAppPerSuite with ViaProxy {
  val client = fromInstanceCache[WSClient]

  "ViaProxy" should {

    "add proxy when it has only required properties" in {
      implicit val cfg: Configuration = Configuration.from(
        Map(
          ProxyHost -> "example.com",
          ProxyPort -> "9898"
        )
      )

      val request = client.url("my.service.example.com").viaProxy()

      request.proxyServer mustBe Some(
        DefaultWSProxyServer(
          host = "example.com",
          port = 9898
        )
      )
    }

    "add proxy when it has all properties" in {
      implicit val cfg: Configuration = Configuration.from(
        Map(
          ProxyHost     -> "example.com",
          ProxyPort     -> "9898",
          ProxyUser     -> "musit",
          ProxyPassword -> "secret"
        )
      )

      val request = client.url("my.service.example.com").viaProxy()

      request.proxyServer mustBe Some(
        DefaultWSProxyServer(
          host = "example.com",
          port = 9898,
          principal = Some("musit"),
          password = Some("secret")
        )
      )
    }

    "not add proxy when missing port config" in {
      implicit val cfg: Configuration =
        Configuration.from(Map(ProxyHost -> "example.com"))

      val request = client.url("my.service.example.com").viaProxy()

      request.proxyServer mustBe None
    }

    "not add proxy when missing host config" in {
      implicit val cfg: Configuration =
        Configuration.from(Map(ProxyPort -> "9898"))

      val request = client.url("my.service.example.com").viaProxy()

      request.proxyServer mustBe None
    }

    "not add proxy when missing config" in {
      implicit val cfg: Configuration = Configuration.from(Map())

      val request = client.url("my.service.example.com").viaProxy()

      request.proxyServer mustBe None
    }

    "continue building after proxy" in {
      implicit val cfg: Configuration = Configuration.from(Map())

      val request =
        client.url("my.service.example.com").viaProxy().withHeaders("foo" -> "bar")

      request.headers mustBe Map("foo" -> List("bar"))
    }
  }

}
