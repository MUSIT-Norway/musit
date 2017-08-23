package no.uio.musit.ws

import no.uio.musit.ws.ProxiedRequest._
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSProxyServer, WSRequest}

class ProxiedRequest(req: WSRequest, config: Configuration) {

  val proxy: Option[WSProxyServer] = for {
    host <- config.getOptional[String](ProxyHost)
    port <- config.getOptional[Int](ProxyPort)
    user     = config.getOptional[String](ProxyUser)
    password = config.getOptional[String](ProxyPassword)
  } yield
    DefaultWSProxyServer(host = host, port = port, principal = user, password = password)

  def viaProxy: WSRequest = proxy.map(req.withProxyServer).getOrElse(req)

}

object ProxiedRequest {

  private[this] val MusitWsProxyKey = "musit.ws.proxy"

  val ProxyHost     = s"$MusitWsProxyKey.host"
  val ProxyPort     = s"$MusitWsProxyKey.port"
  val ProxyUser     = s"$MusitWsProxyKey.user"
  val ProxyPassword = s"$MusitWsProxyKey.password"

}

trait ViaProxy {

  implicit def viaProxy(req: WSRequest)(implicit config: Configuration): ProxiedRequest =
    new ProxiedRequest(req, config)

}

object ViaProxy extends ViaProxy
